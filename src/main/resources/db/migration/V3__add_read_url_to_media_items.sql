-- V3: Add read_url column to media_items for permanent public access URL
ALTER TABLE media_items
    ADD COLUMN read_url VARCHAR(1000) NOT NULL DEFAULT '' AFTER file_key;
