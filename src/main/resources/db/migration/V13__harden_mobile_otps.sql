-- OTP codes are now stored as a SHA-256 hex digest (64 chars) instead of plain text.
-- Any OTP issued before this migration can no longer be verified; they expire after 5 minutes anyway.
ALTER TABLE mobile_otps
    ALTER COLUMN code TYPE VARCHAR(64),
    ADD COLUMN last_sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;
