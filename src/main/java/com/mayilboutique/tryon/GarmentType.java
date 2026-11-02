package com.mayilboutique.tryon;

/** How the AI should treat the garment. Providers are chosen per type, since no model handles all of them equally well. */
public enum GarmentType {
    UPPER_BODY,   // kurtis, tops, shirts, blouses
    SALWAR_SUIT,
    SAREE,
    LEHENGA,
    DRESS
}
