-- A digital variant's download: the file, how many times a buyer may fetch
-- it and for how many days a link lives.
CREATE TABLE digital_asset (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    file_id VARCHAR(32) NOT NULL,
    max_downloads INTEGER NOT NULL,
    expiry_days INTEGER NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX digital_asset_variant ON digital_asset (variant_id);
CREATE INDEX digital_asset_store ON digital_asset (store_id);

-- What a paid order's buyer may download: one row per order line, keyed by
-- a random secret the signed link is checked against.
CREATE TABLE download_grant (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    line_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    file_id VARCHAR(32) NOT NULL,
    username VARCHAR(200) NOT NULL,
    secret VARCHAR(64) NOT NULL,
    expires_at BIGINT NOT NULL,
    downloads INTEGER NOT NULL,
    max_downloads INTEGER NOT NULL,
    license_key VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX download_grant_line ON download_grant (line_id);
CREATE INDEX download_grant_order ON download_grant (order_id);
CREATE INDEX download_grant_store ON download_grant (store_id);

-- A pool of licence keys for a variant; a sale takes the next free one.
CREATE TABLE license_key (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    key_text VARCHAR(200) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    assigned_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX license_key_variant ON license_key (variant_id, order_id);
CREATE INDEX license_key_store ON license_key (store_id);
