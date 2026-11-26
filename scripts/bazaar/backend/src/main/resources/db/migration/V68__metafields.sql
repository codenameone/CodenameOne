-- Wave C: merchant-defined typed fields and custom object types.
CREATE TABLE metafield_def (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    owner_kind VARCHAR(40) NOT NULL,
    field_key VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    field_type VARCHAR(16) NOT NULL,
    ref_kind VARCHAR(16) NOT NULL,
    storefront SMALLINT NOT NULL,
    position INTEGER NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX metafield_def_store_owner_key ON metafield_def (store_id, owner_kind, field_key);
CREATE TABLE metafield_value (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    def_id VARCHAR(32) NOT NULL,
    owner_id VARCHAR(32) NOT NULL,
    text_value TEXT NOT NULL,
    number_value BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX metafield_value_def_owner_key ON metafield_value (def_id, owner_id);
CREATE INDEX metafield_value_store_owner ON metafield_value (store_id, owner_id);
CREATE TABLE metaobject_type (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    handle VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX metaobject_type_store_handle_key ON metaobject_type (store_id, handle);
CREATE TABLE metaobject_entry (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    type_id VARCHAR(32) NOT NULL,
    handle VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX metaobject_entry_type ON metaobject_entry (type_id);
