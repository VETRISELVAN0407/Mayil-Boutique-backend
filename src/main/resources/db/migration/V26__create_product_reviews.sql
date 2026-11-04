CREATE TABLE product_reviews (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INTEGER NOT NULL,
    comment VARCHAR(1000),
    -- Snapshot from the purchased order line, so the review keeps its colour/size and a display name.
    reviewer_name VARCHAR(100) NOT NULL,
    colour VARCHAR(40),
    size VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_product_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT uk_product_reviews_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_product_reviews_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_reviews_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_reviews_product_id ON product_reviews (product_id);

CREATE TABLE review_helpful_votes (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_review_helpful_review_user UNIQUE (review_id, user_id),
    CONSTRAINT fk_review_helpful_review FOREIGN KEY (review_id) REFERENCES product_reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_helpful_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
