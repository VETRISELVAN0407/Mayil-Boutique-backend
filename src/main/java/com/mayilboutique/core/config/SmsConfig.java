package com.mayilboutique.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import com.mayilboutique.auth.service.DummyOtpSender;
import com.mayilboutique.auth.service.LogOtpSender;
import com.mayilboutique.auth.service.Msg91OtpSender;
import com.mayilboutique.auth.service.OtpSender;
import com.mayilboutique.auth.service.UnconfiguredOtpSender;

@Configuration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsConfig {

    private static final Logger log = LoggerFactory.getLogger(SmsConfig.class);

    /**
     * With SMS_PROVIDER=msg91 but a missing key/template the app still starts (admin, catalog and
     * checkout keep working); only OTP requests fail, with the missing variable named in the log.
     */
    @Bean
    @ConditionalOnProperty(name = "app.sms.provider", havingValue = "msg91")
    OtpSender msg91OtpSender(SmsProperties properties) {
        SmsProperties.Msg91 msg91 = properties.getMsg91();
        StringBuilder missing = new StringBuilder();
        if (isBlank(msg91.getAuthKey())) {
            missing.append("MSG91_AUTH_KEY ");
        }
        if (isBlank(msg91.getTemplateId())) {
            missing.append("MSG91_TEMPLATE_ID ");
        }
        if (missing.length() > 0) {
            String reason = "SMS_PROVIDER=msg91 but " + missing.toString().trim() + " is not set in backend/.env";
            log.error("{}. OTP requests will fail until it is set.", reason);
            return new UnconfiguredOtpSender(reason);
        }
        log.info("OTP delivery: MSG91 (template {})", msg91.getTemplateId());
        return new Msg91OtpSender(msg91);
    }

    @Bean
    @ConditionalOnProperty(name = "app.sms.provider", havingValue = "log")
    OtpSender logOtpSender() {
        log.warn("OTP delivery: LOG provider - OTPs are printed to the console and NOT sent. Local development only.");
        return new LogOtpSender();
    }

    /**
     * TEMPORARY test mode: the OTP is shown on the login page instead of being delivered. Never
     * allowed when the prod profile is active - the application refuses to start.
     */
    @Bean
    @ConditionalOnProperty(name = "app.sms.provider", havingValue = "dummy")
    OtpSender dummyOtpSender(Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "SMS_PROVIDER=dummy (test OTP shown on the login page) is not allowed with the prod profile. "
                            + "Use SMS_PROVIDER=msg91.");
        }
        log.warn("OTP delivery: DUMMY test mode - OTPs are shown on the login page and NOT sent. Development only.");
        return new DummyOtpSender();
    }

    @Bean
    @ConditionalOnProperty(name = "app.sms.provider", havingValue = "none", matchIfMissing = true)
    OtpSender unconfiguredOtpSender() {
        String reason = "no SMS provider configured; set SMS_PROVIDER=msg91 in backend/.env";
        log.error("OTP delivery disabled: {}", reason);
        return new UnconfiguredOtpSender(reason);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
