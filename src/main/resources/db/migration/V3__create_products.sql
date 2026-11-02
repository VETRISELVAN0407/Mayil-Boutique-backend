CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT,
    price DECIMAL(12,2) NOT NULL,
    sale_price DECIMAL(12,2),
    sku VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT uk_products_sku UNIQUE (sku)
);

CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_category_id ON products (category_id);
