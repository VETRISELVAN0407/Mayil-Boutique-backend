CREATE TABLE mobile_otps (
    id BIGSERIAL PRIMARY KEY,
    mobile VARCHAR(20) NOT NULL,
    code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mobile_otps_mobile ON mobile_otps (mobile);
