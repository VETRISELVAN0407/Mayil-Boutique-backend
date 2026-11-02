package com.mayilboutique.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;

import com.mayilboutique.auth.service.DummyOtpSender;
import com.mayilboutique.auth.service.LogOtpSender;
import com.mayilboutique.auth.service.Msg91OtpSender;
import com.mayilboutique.auth.service.OtpSender;
import com.mayilboutique.auth.service.UnconfiguredOtpSender;
import com.mayilboutique.core.exception.BusinessException;

/** Checks that the SMS_PROVIDER / MSG91_* settings select the right OtpSender. */
class SmsConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(SmsConfig.class);

    @Test
    void noProviderConfiguredFailsOtpRequestsInsteadOfPretendingToSend() {
        runner.run(context -> {
            OtpSender sender = context.getBean(OtpSender.class);
            assertThat(sender).isInstanceOf(UnconfiguredOtpSender.class);
            BusinessException ex = assertThrows(BusinessException.class, () -> sender.send("9876543210", "123456"));
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
        });
    }

    @Test
    void msg91WithCredentialsUsesTheMsg91Sender() {
        runner.withPropertyValues(
                        "app.sms.provider=msg91",
                        "app.sms.msg91.auth-key=test-key",
                        "app.sms.msg91.template-id=test-template")
                .run(context -> assertThat(context.getBean(OtpSender.class)).isInstanceOf(Msg91OtpSender.class));
    }

    @Test
    void msg91WithBlankCredentialsStillStartsButOtpRequestsFail() {
        runner.withPropertyValues("app.sms.provider=msg91", "app.sms.msg91.auth-key=", "app.sms.msg91.template-id=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OtpSender sender = context.getBean(OtpSender.class);
                    assertThat(sender).isInstanceOf(UnconfiguredOtpSender.class);
                    assertThrows(BusinessException.class, () -> sender.send("9876543210", "123456"));
                });
    }

    @Test
    void msg91WithOnlyTheKeyMissingIsAlsoTreatedAsUnconfigured() {
        runner.withPropertyValues("app.sms.provider=msg91", "app.sms.msg91.template-id=test-template")
                .run(context -> assertThat(context.getBean(OtpSender.class)).isInstanceOf(UnconfiguredOtpSender.class));
    }

    @Test
    void logProviderIsOnlyUsedWhenExplicitlyRequested() {
        runner.withPropertyValues("app.sms.provider=log")
                .run(context -> assertThat(context.getBean(OtpSender.class)).isInstanceOf(LogOtpSender.class));
    }

    @Test
    void dummyProviderRevealsCodeAndIsNeverTheDefault() {
        runner.run(context -> assertThat(context.getBean(OtpSender.class).revealsCode()).isFalse());
        runner.withPropertyValues("app.sms.provider=dummy").run(context -> {
            OtpSender sender = context.getBean(OtpSender.class);
            assertThat(sender).isInstanceOf(DummyOtpSender.class);
            assertThat(sender.revealsCode()).isTrue();
        });
        runner.withPropertyValues("app.sms.provider=msg91", "app.sms.msg91.auth-key=k", "app.sms.msg91.template-id=t")
                .run(context -> assertThat(context.getBean(OtpSender.class).revealsCode()).isFalse());
    }

    @Test
    void dummyProviderIsRefusedWithTheProdProfile() {
        runner.withPropertyValues("app.sms.provider=dummy", "spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "SMS_PROVIDER=dummy (test OTP shown on the login page) is not allowed with the prod profile. "
                                    + "Use SMS_PROVIDER=msg91.");
                });
    }
}
