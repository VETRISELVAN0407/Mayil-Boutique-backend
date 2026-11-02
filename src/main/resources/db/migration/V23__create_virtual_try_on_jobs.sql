-- One row per Virtual Try-On generation. Only file NAMES are stored (in a private folder); rows outlive the files,
-- which are deleted when expires_at passes (or the customer deletes the job).
CREATE TABLE virtual_try_on_jobs (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    garment_type VARCHAR(32) NOT NULL,
    provider VARCHAR(64),
    provider_job_id VARCHAR(255),
    status VARCHAR(16) NOT NULL,
    original_photo_path VARCHAR(255),
    generated_image_path VARCHAR(255),
    error_message VARCHAR(500),
    consent_accepted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_tryon_jobs_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_tryon_jobs_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL
);

CREATE INDEX idx_tryon_jobs_customer_created ON virtual_try_on_jobs (customer_id, created_at DESC);
CREATE INDEX idx_tryon_jobs_status_expires ON virtual_try_on_jobs (status, expires_at);
