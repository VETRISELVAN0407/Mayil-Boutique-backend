-- Scheduled sale campaigns: a percentage off, for all products, a category and/or chosen products.
CREATE TABLE sales (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    percent_off  NUMERIC(5, 2) NOT NULL CHECK (percent_off > 0 AND percent_off <= 90),
    all_products BOOLEAN NOT NULL DEFAULT FALSE,
    category_id  BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    starts_at    TIMESTAMPTZ NOT NULL,
    ends_at      TIMESTAMPTZ NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (ends_at > starts_at)
);

CREATE TABLE sale_products (
    sale_id    BIGINT NOT NULL REFERENCES sales (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    PRIMARY KEY (sale_id, product_id)
);

CREATE INDEX idx_sales_window ON sales (active, starts_at, ends_at);

-- Coupon codes (stored upper-case).
CREATE TABLE coupons (
    id                BIGSERIAL PRIMARY KEY,
    code              VARCHAR(32) NOT NULL UNIQUE,
    description       VARCHAR(255),
    type              VARCHAR(20) NOT NULL CHECK (type IN ('PERCENT', 'FLAT', 'FREE_SHIPPING')),
    value             NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (value >= 0),
    max_discount      NUMERIC(12, 2) CHECK (max_discount IS NULL OR max_discount > 0),
    min_order_value   NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (min_order_value >= 0),
    starts_at         TIMESTAMPTZ,
    ends_at           TIMESTAMPTZ,
    usage_limit       INTEGER CHECK (usage_limit IS NULL OR usage_limit > 0),
    used_count        INTEGER NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    once_per_customer BOOLEAN NOT NULL DEFAULT FALSE,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- One row per order that used a coupon; removed again when the order is cancelled.
CREATE TABLE coupon_redemptions (
    id         BIGSERIAL PRIMARY KEY,
    coupon_id  BIGINT NOT NULL REFERENCES coupons (id),
    user_id    BIGINT NOT NULL,
    order_id   BIGINT NOT NULL UNIQUE REFERENCES orders (id) ON DELETE CASCADE,
    discount   NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_coupon_redemptions_coupon_user ON coupon_redemptions (coupon_id, user_id);

ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(32);
