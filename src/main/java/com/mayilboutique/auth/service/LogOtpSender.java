package com.mayilboutique.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LOCAL DEVELOPMENT ONLY (app.sms.provider=log): prints the OTP to the backend console and sends
 * nothing. It is never selected by default.
 */
public class LogOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LogOtpSender.class);

    @Override
    public void send(String mobile, String code) {
        log.warn("DEV ONLY - no SMS sent. OTP for {} is {}", mobile, code);
    }
}
