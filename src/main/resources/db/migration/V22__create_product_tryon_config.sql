-- Per-product Virtual Try-On settings. A product without a row is not enabled for try-on.
CREATE TABLE product_tryon_config (
    product_id BIGINT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    garment_type VARCHAR(32),
    ai_ready BOOLEAN NOT NULL DEFAULT FALSE,
    primary_image_id BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tryon_config_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    -- Removing the image from the gallery leaves the product without an AI image; listings treat it as not eligible.
    CONSTRAINT fk_tryon_config_primary_image FOREIGN KEY (primary_image_id) REFERENCES product_images (id) ON DELETE SET NULL
);

CREATE INDEX idx_tryon_config_eligible ON product_tryon_config (enabled, ai_ready);

-- Extra garment reference images (back view, detail, ...), taken from the product's own gallery.
CREATE TABLE product_tryon_reference_images (
    product_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, image_id),
    CONSTRAINT fk_tryon_ref_config FOREIGN KEY (product_id) REFERENCES product_tryon_config (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_tryon_ref_image FOREIGN KEY (image_id) REFERENCES product_images (id) ON DELETE CASCADE
);
