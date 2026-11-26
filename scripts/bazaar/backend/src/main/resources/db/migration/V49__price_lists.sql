-- Wave B: B2B price lists and per-variant quantity rules.
CREATE TABLE price_list (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    percent_off_bps INTEGER NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE TABLE price_list_entry (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    price_list_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    fixed_cents BIGINT NOT NULL,
    min_qty INTEGER NOT NULL,
    max_qty INTEGER NOT NULL,
    step_qty INTEGER NOT NULL
);
CREATE INDEX price_list_store_id ON price_list (store_id);
CREATE INDEX price_list_entry_list_variant ON price_list_entry (price_list_id, variant_id);
CREATE INDEX price_list_entry_store_id ON price_list_entry (store_id);
