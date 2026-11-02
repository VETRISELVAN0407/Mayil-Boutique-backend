package com.mayilboutique.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mayilboutique.auth.entity.MobileOtp;
import com.mayilboutique.auth.repository.MobileOtpRepository;
import com.mayilboutique.auth.repository.UserRepository;
import com.mayilboutique.core.entity.User;
import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.exception.BusinessException;

class OtpServiceTest {

    private static final String MOBILE = "9876543210";

    private MobileOtpRepository otpRepository;
    private UserRepository userRepository;
    private OtpSender sender;
    private OtpService service;

    @BeforeEach
    void setUp() {
        otpRepository = mock(MobileOtpRepository.class);
        userRepository = mock(UserRepository.class);
        sender = mock(OtpSender.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(otpRepository.save(any(MobileOtp.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new OtpService(otpRepository, userRepository, sender, encoder);
    }

    /** Issues an OTP and returns the plain code that was handed to the sender. */
    private String issueOtp(MobileOtp[] stored) {
        when(otpRepository.findTopByMobileOrderByIdDesc(MOBILE)).thenReturn(Optional.empty());
        service.generateAndSendOtp(MOBILE);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(sender).send(org.mockito.ArgumentMatchers.eq(MOBILE), code.capture());
        ArgumentCaptor<MobileOtp> saved = ArgumentCaptor.forClass(MobileOtp.class);
        verify(otpRepository).save(saved.capture());
        stored[0] = saved.getValue();
        when(otpRepository.findTopByMobileOrderByIdDesc(MOBILE)).thenReturn(Optional.of(stored[0]));
        return code.getValue();
    }

    @Test
    void sentCodeIsSixDigitsAndStoredOnlyAsHash() {
        MobileOtp[] stored = new MobileOtp[1];
        String code = issueOtp(stored);

        assertTrue(code.matches("\\d{6}"));
        assertNotEquals(code, stored[0].getCode());
        assertEquals(64, stored[0].getCode().length());
    }

    @Test
    void revealedCodeIsTheSameOneThatVerifiesAndOnlyWhenSenderRevealsIt() {
        when(otpRepository.findTopByMobileOrderByIdDesc(MOBILE)).thenReturn(Optional.empty());
        assertTrue(service.generateAndSendOtp(MOBILE).isEmpty());

        OtpSender dummy = new DummyOtpSender();
        service = new OtpService(otpRepository, userRepository, dummy, mock(PasswordEncoder.class));
        ArgumentCaptor<MobileOtp> saved = ArgumentCaptor.forClass(MobileOtp.class);
        String shown = service.generateAndSendOtp(MOBILE).orElseThrow();
        verify(otpRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        MobileOtp stored = saved.getValue();

        assertTrue(shown.matches("[0-9]{6}"));
        when(otpRepository.findTopByMobileOrderByIdDesc(MOBILE)).thenReturn(Optional.of(stored));
        when(userRepository.findByMobile(MOBILE)).thenReturn(Optional.empty());
        assertEquals(Role.CUSTOMER, service.verifyOtp(MOBILE, shown).getRole());
    }

    @Test
    void plusPrefixIsStrippedBeforeStoringAndSending() {
        when(otpRepository.findTopByMobileOrderByIdDesc(MOBILE)).thenReturn(Optional.empty());

        service.generateAndSendOtp("+" + MOBILE);

        verify(sender).send(org.mockito.ArgumentMatchers.eq(MOBILE), anyString());
    }

    @Test
    void deliveryFailureIsPropagatedSoNothingIsReportedAsSent() {
        when(otpRepository.findTopByMobileOrderByIdDesc(MOBILE)).thenReturn(Optional.empty());
        doThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "boom")).when(sender).send(anyString(), anyString());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.generateAndSendOtp(MOBILE));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getHttpStatus());
    }

    @Test
    void resendWithinCooldownIsRejectedAndNothingIsSent() {
        MobileOtp recent = new MobileOtp(MOBILE, "hash", Instant.now().plusSeconds(300));
        recent.setLastSentAt(Instant.now().minusSeconds(5));
        when(otpRepository.findTopByMobileOrderByIdDesc(MOBILE)).thenReturn(Optional.of(recent));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.generateAndSendOtp(MOBILE));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getHttpStatus());
        verify(sender, never()).send(anyString(), anyString());
    }

    @Test
    void correctCodeRegistersNewCustomerAndIsSingleUse() {
        MobileOtp[] stored = new MobileOtp[1];
        String code = issueOtp(stored);
        when(userRepository.findByMobile(MOBILE)).thenReturn(Optional.empty());

        User user = service.verifyOtp(MOBILE, code);

        assertEquals(MOBILE, user.getMobile());
        assertEquals(Role.CUSTOMER, user.getRole());
        assertTrue(stored[0].isConsumed());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.verifyOtp(MOBILE, code));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void wrongCodeCountsAttemptAndLocksAfterFiveFailures() {
        MobileOtp[] stored = new MobileOtp[1];
        String code = issueOtp(stored);
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThrows(BusinessException.class, () -> service.verifyOtp(MOBILE, wrong));
        }
        assertEquals(5, stored[0].getFailedAttempts());

        // Even the correct code is refused once the attempt limit is hit.
        assertThrows(BusinessException.class, () -> service.verifyOtp(MOBILE, code));
        assertTrue(!stored[0].isConsumed());
    }

    @Test
    void expiredCodeIsRejected() {
        MobileOtp[] stored = new MobileOtp[1];
        String code = issueOtp(stored);
        stored[0].setExpiresAt(Instant.now().minusSeconds(1));

        assertThrows(BusinessException.class, () -> service.verifyOtp(MOBILE, code));
    }
}
