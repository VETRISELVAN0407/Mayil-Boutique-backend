-- Uploaded category picture (generated filename in the shared image folder). The older free-text `image` URL stays
-- as a fallback for categories that have no upload.
ALTER TABLE categories ADD COLUMN image_filename VARCHAR(64);
