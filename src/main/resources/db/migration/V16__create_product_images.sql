-- Several images per product; the lowest sort_order is the cover.
CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_product_images_filename UNIQUE (filename),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id, sort_order);

-- The single image column becomes the first row; the table is then the only source of truth.
INSERT INTO product_images (product_id, filename, sort_order)
SELECT id, image_filename, 0 FROM products WHERE image_filename IS NOT NULL;

ALTER TABLE products DROP COLUMN image_filename;

-- Order lines keep the cover as it was when the order was placed, so old orders keep their picture.
ALTER TABLE order_items ADD COLUMN image_filename VARCHAR(255);

-- Best effort for orders placed before this migration: the product's current cover.
UPDATE order_items oi
SET image_filename = pi.filename
FROM product_variants v
JOIN product_images pi ON pi.product_id = v.product_id AND pi.sort_order = 0
WHERE oi.variant_id = v.id;
