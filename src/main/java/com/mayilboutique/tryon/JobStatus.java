package com.mayilboutique.tryon;

public enum JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED,
    CANCELLED;

    public boolean isActive() {
        return this == PENDING || this == PROCESSING;
    }
}
