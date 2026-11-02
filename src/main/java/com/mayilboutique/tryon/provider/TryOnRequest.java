package com.mayilboutique.tryon.provider;

import java.util.List;

import com.mayilboutique.tryon.GarmentType;

/** {@code garments} holds the AI-ready image first, then any extra reference images (engines may ignore extras). */
public record TryOnRequest(GarmentType garmentType, byte[] personPhoto, List<byte[]> garments) {
}
