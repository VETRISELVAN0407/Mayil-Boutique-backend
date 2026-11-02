CREATE TABLE stock_updates (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    stock_added INTEGER NOT NULL,
    total_stock INTEGER NOT NULL,
    updated_on TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_stock_updates_added_positive CHECK (stock_added > 0),
    CONSTRAINT ck_stock_updates_total_nonnegative CHECK (total_stock >= 0),
    CONSTRAINT fk_stock_updates_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_updates_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE
);

CREATE INDEX idx_stock_updates_updated_on ON stock_updates (updated_on DESC, id DESC);
CREATE INDEX idx_stock_updates_variant_id ON stock_updates (variant_id);
