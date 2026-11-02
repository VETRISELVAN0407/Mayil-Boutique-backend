package com.mayilboutique.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TEMPORARY TEST MODE (app.sms.provider=dummy): nothing is sent to SMS/WhatsApp; the generated OTP
 * is returned in the OTP request response so the login page can display it. It is the normal
 * generated, hashed, expiring, single-use OTP - there is no fixed or universal code. Refused at
 * startup under the prod profile (see SmsConfig).
 */
public class DummyOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(DummyOtpSender.class);

    @Override
    public void send(String mobile, String code) {
        log.warn("TEST MODE - no SMS/WhatsApp sent. OTP for {} is shown on the login page.", mobile);
    }

    @Override
    public boolean revealsCode() {
        return true;
    }
}
