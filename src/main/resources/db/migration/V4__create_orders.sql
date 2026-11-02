CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_ref VARCHAR(64) NOT NULL,
    customer_mobile VARCHAR(20) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    shipping_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    total DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_orders_order_ref UNIQUE (order_ref)
);

CREATE INDEX idx_orders_customer_mobile ON orders (customer_mobile);
CREATE INDEX idx_orders_status ON orders (status);
