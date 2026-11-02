package com.mayilboutique.tryon.provider;

import com.mayilboutique.tryon.GarmentType;

/**
 * An AI engine that turns (customer photo + garment image) into a try-on picture. Implementations are chosen by
 * {@code app.tryon.provider}; the rest of the app only talks to this interface. Calls are made from the backend
 * only, so credentials never reach the browser.
 */
public interface VirtualTryOnProvider {

    /** Short name stored on the job, e.g. {@code selfhosted}. */
    String name();

    /** Whether this engine can handle the garment type. Unsupported types are never offered to customers. */
    boolean supports(GarmentType type);

    /** Submits a generation and returns immediately with the provider's job id. */
    String start(TryOnRequest request) throws ProviderException;

    ProviderStatus status(String providerJobId) throws ProviderException;

    /** The generated image bytes; only valid once {@link #status} reported DONE. */
    byte[] result(String providerJobId) throws ProviderException;

    /** Removes the provider-side copy of the inputs and result. Best effort: must not throw for unknown ids. */
    void delete(String providerJobId);
}
