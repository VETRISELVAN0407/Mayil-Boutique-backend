package com.mayilboutique.tryon.provider;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.TryOnProperties;

/**
 * What the connected engine can do. With no engine configured everything is accepted in the admin settings (so
 * products can be prepared in advance); with one, only the garment types it supports are allowed and shown.
 */
@Component
public class TryOnCapabilities {

    private final ObjectProvider<VirtualTryOnProvider> provider;
    private final TryOnProperties props;

    public TryOnCapabilities(ObjectProvider<VirtualTryOnProvider> provider, TryOnProperties props) {
        this.provider = provider;
        this.props = props;
    }

    /** Customers can use Virtual Try-On only when it is switched on AND an engine is connected. */
    public boolean available() {
        return props.isEnabled() && providerConfigured();
    }

    public boolean providerConfigured() {
        return provider.getIfAvailable() != null;
    }

    public boolean isSupported(GarmentType type) {
        VirtualTryOnProvider p = provider.getIfAvailable();
        return p == null || p.supports(type);
    }

    public List<GarmentType> supportedTypes() {
        return Arrays.stream(GarmentType.values()).filter(this::isSupported).toList();
    }
}
