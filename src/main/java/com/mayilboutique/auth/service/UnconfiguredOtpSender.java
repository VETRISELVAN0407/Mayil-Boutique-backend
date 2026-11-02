package com.mayilboutique.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.mayilboutique.core.exception.BusinessException;

/** Used when SMS is not (fully) configured: fails loudly instead of pretending an OTP was sent. */
public class UnconfiguredOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(UnconfiguredOtpSender.class);

    private final String reason;

    public UnconfiguredOtpSender(String reason) {
        this.reason = reason;
    }

    @Override
    public void send(String mobile, String code) {
        log.error("OTP requested but SMS delivery is not available: {}", reason);
        throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                "OTP login is temporarily unavailable. Please try again later.");
    }
}
