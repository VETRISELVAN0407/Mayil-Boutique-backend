package com.mayilboutique.auth.service;

public interface OtpSender {
    void send(String mobile, String code);

    /**
     * True only for the development-only dummy sender: the code is then returned to the caller of
     * the OTP request instead of being delivered. Every real sender keeps the default {@code false}.
     */
    default boolean revealsCode() {
        return false;
    }
}
