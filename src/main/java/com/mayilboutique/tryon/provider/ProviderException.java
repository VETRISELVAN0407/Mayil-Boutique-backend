package com.mayilboutique.tryon.provider;

/**
 * A provider call failed. The message is for logs only and never shown to customers. {@code retryable} is true for
 * timeouts, 5xx and rate limiting, false for rejected input or an unknown job.
 */
public class ProviderException extends Exception {

    private final boolean retryable;

    public ProviderException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
