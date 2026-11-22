-- A store's API keys. Only the SHA-256 of a key is kept; the prefix and last
-- four characters are enough to tell keys apart in a list.
CREATE TABLE store_api_key (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(80) NOT NULL,
    scopes VARCHAR(400) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    prefix VARCHAR(16) NOT NULL,
    last_four VARCHAR(4) NOT NULL,
    created_by VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL,
    last_used_at BIGINT NOT NULL,
    revoked_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX store_api_key_hash ON store_api_key (key_hash);
CREATE INDEX store_api_key_store_id ON store_api_key (store_id);

-- An asynchronous bulk operation: an export of products or orders as JSON
-- Lines, or an import of products from them.
CREATE TABLE bulk_job (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    requested_by VARCHAR(200) NOT NULL,
    input TEXT NOT NULL,
    output TEXT NOT NULL,
    errors TEXT NOT NULL,
    line_count INTEGER NOT NULL,
    error_count INTEGER NOT NULL,
    created_at BIGINT NOT NULL,
    finished_at BIGINT NOT NULL
);
CREATE INDEX bulk_job_store_id ON bulk_job (store_id);
CREATE INDEX bulk_job_status ON bulk_job (status);
