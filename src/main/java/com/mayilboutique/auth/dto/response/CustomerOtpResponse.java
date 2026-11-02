package com.mayilboutique.auth.dto.response;

/** Only returned in the development-only dummy OTP mode; real delivery answers with an empty body. */
public record CustomerOtpResponse(boolean testMode, String testOtp, String message) {
}
