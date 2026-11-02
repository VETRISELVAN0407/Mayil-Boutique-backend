ALTER TABLE orders
    ADD COLUMN user_id BIGINT,
    ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'COD',
    ADD COLUMN ship_name VARCHAR(255),
    ADD COLUMN ship_mobile VARCHAR(20),
    ADD COLUMN ship_line1 VARCHAR(255),
    ADD COLUMN ship_line2 VARCHAR(255),
    ADD COLUMN ship_city VARCHAR(100),
    ADD COLUMN ship_state VARCHAR(100),
    ADD COLUMN ship_pincode VARCHAR(10),
    ADD CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_orders_user_id ON orders (user_id);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    variant_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    size VARCHAR(20),
    colour VARCHAR(40),
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total DECIMAL(12,2) NOT NULL,
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE SET NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
