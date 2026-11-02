package com.mayilboutique.tryon.provider;

public record ProviderStatus(State state) {

    public enum State { QUEUED, RUNNING, DONE, FAILED }
}
