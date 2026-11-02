-- Durable copy of uploaded product/category images, used when the app runs on a host with an ephemeral disk
-- (app.images.persist-in-db=true). The files on disk stay the working copy; a missing file is restored from here.
CREATE TABLE stored_images (
    filename   VARCHAR(64) PRIMARY KEY,
    content    BYTEA       NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
