package com.mayilboutique.tryon.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Deletes the provider-side copy of a job's pictures; never throws, so it cannot break the caller's own cleanup. */
@Component
public class ProviderAssets {

    private static final Logger log = LoggerFactory.getLogger(ProviderAssets.class);

    private final ObjectProvider<VirtualTryOnProvider> provider;

    public ProviderAssets(ObjectProvider<VirtualTryOnProvider> provider) {
        this.provider = provider;
    }

    public void deleteQuietly(String providerJobId) {
        VirtualTryOnProvider p = provider.getIfAvailable();
        if (p == null || providerJobId == null) {
            return;
        }
        try {
            p.delete(providerJobId);
        } catch (RuntimeException ex) {
            log.warn("Could not delete provider-side try-on data ({})", ex.getClass().getSimpleName());
        }
    }
}
