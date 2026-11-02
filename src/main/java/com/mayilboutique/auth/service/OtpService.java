package com.mayilboutique.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.auth.entity.MobileOtp;
import com.mayilboutique.auth.repository.MobileOtpRepository;
import com.mayilboutique.auth.repository.UserRepository;
import com.mayilboutique.core.entity.User;
import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.enums.UserStatus;
import com.mayilboutique.core.exception.BusinessException;

@Service
public class OtpService {

    private static final int OTP_CODE_DIGITS = 6;
    private static final Duration OTP_VALIDITY = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final String INVALID_OTP = "Invalid or expired OTP";

    private final SecureRandom random = new SecureRandom();
    private final MobileOtpRepository mobileOtpRepository;
    private final UserRepository userRepository;
    private final OtpSender otpSender;
    private final PasswordEncoder passwordEncoder;

    public OtpService(MobileOtpRepository mobileOtpRepository, UserRepository userRepository,
                      OtpSender otpSender, PasswordEncoder passwordEncoder) {
        this.mobileOtpRepository = mobileOtpRepository;
        this.userRepository = userRepository;
        this.otpSender = otpSender;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Issues a fresh OTP and sends it. If delivery fails the transaction rolls back, so no OTP
     * (and no resend cooldown) is recorded for a code the customer never received.
     *
     * @return the plain code only when the sender is the development-only dummy sender, else empty
     */
    @Transactional
    public Optional<String> generateAndSendOtp(String rawMobile) {
        String mobile = normalize(rawMobile);
        Instant now = Instant.now();

        MobileOtp otp = mobileOtpRepository.findTopByMobileOrderByIdDesc(mobile).orElse(null);
        if (otp != null && otp.getLastSentAt().plus(RESEND_COOLDOWN).isAfter(now)) {
            long wait = Duration.between(now, otp.getLastSentAt().plus(RESEND_COOLDOWN)).toSeconds() + 1;
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait " + wait + " seconds before requesting another OTP");
        }

        String code = String.format("%0" + OTP_CODE_DIGITS + "d", random.nextInt((int) Math.pow(10, OTP_CODE_DIGITS)));
        if (otp == null) {
            otp = new MobileOtp(mobile, hash(mobile, code), now.plus(OTP_VALIDITY));
        } else {
            otp.setCode(hash(mobile, code));
            otp.setExpiresAt(now.plus(OTP_VALIDITY));
            otp.setConsumed(false);
            otp.setFailedAttempts(0);
        }
        otp.setLastSentAt(now);
        mobileOtpRepository.save(otp);

        otpSender.send(mobile, code);
        return otpSender.revealsCode() ? Optional.of(code) : Optional.empty();
    }

    /**
     * Wrong codes must still count as failed attempts, so a {@link BusinessException} must not roll
     * back the attempt counter.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public User verifyOtp(String rawMobile, String code) {
        String mobile = normalize(rawMobile);
        MobileOtp otp = mobileOtpRepository.findTopByMobileOrderByIdDesc(mobile)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_OTP));

        if (otp.isConsumed() || otp.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_OTP);
        }
        if (otp.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Too many incorrect attempts. Please request a new OTP");
        }
        if (!MessageDigest.isEqual(
                otp.getCode().getBytes(StandardCharsets.UTF_8),
                hash(mobile, code).getBytes(StandardCharsets.UTF_8))) {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            mobileOtpRepository.save(otp);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_OTP);
        }

        otp.setConsumed(true);
        mobileOtpRepository.save(otp);

        // First successful verification registers the customer; the password is never used (OTP-only login).
        return userRepository.findByMobile(mobile).orElseGet(() -> userRepository.save(
                new User(mobile, passwordEncoder.encode(UUID.randomUUID().toString()), Role.CUSTOMER, UserStatus.ACTIVE)));
    }

    /** Request DTOs allow an optional leading "+"; storage and delivery use the bare 10 digits. */
    static String normalize(String mobile) {
        return mobile.startsWith("+") ? mobile.substring(1) : mobile;
    }

    private static String hash(String mobile, String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((mobile + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
