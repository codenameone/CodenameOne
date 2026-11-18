-- Place files and data store values: large columns, so their types differ per
-- database. This is the sqlite version.

CREATE TABLE experience_version (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    version_number INTEGER NOT NULL,
    place BLOB NOT NULL,
    place_size INTEGER NOT NULL,
    report TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX experience_version_number ON experience_version (experience_id, version_number);

CREATE TABLE data_store_entry (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    store_name VARCHAR(64) NOT NULL,
    scope VARCHAR(64) NOT NULL,
    entry_key VARCHAR(128) NOT NULL,
    json_value TEXT NOT NULL,
    revision BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX data_store_entry_key ON data_store_entry (experience_id, store_name, scope, entry_key);
