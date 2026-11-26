-- Wave C: the warehouse board, bins and the timeline of transfers and purchase orders.
CREATE TABLE warehouse_bin (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    code VARCHAR(32) NOT NULL,
    aisle INTEGER NOT NULL,
    shelf INTEGER NOT NULL,
    position INTEGER NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX warehouse_bin_code ON warehouse_bin (location_id, code);
CREATE INDEX warehouse_bin_store ON warehouse_bin (store_id);
CREATE TABLE warehouse_task (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    ref_id VARCHAR(32) NOT NULL,
    label VARCHAR(120) NOT NULL,
    stage VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX warehouse_task_ref ON warehouse_task (location_id, kind, ref_id);
CREATE INDEX warehouse_task_store ON warehouse_task (store_id, location_id);
CREATE TABLE ops_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    subject_kind VARCHAR(16) NOT NULL,
    subject_id VARCHAR(32) NOT NULL,
    kind VARCHAR(24) NOT NULL,
    message VARCHAR(300) NOT NULL,
    username VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX ops_event_subject ON ops_event (subject_id);
