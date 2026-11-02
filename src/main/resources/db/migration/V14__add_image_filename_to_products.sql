-- Product images live on disk (app.images.dir); only the generated filename is stored here.
-- Nullable: existing products simply have no image yet.
ALTER TABLE products
    ADD COLUMN image_filename VARCHAR(255);
