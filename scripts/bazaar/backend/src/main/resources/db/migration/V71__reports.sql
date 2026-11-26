-- Wave C: saved custom reports and first/last touch attribution of carts and orders.
CREATE TABLE saved_report (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    spec VARCHAR(1000) NOT NULL,
    created_by VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX saved_report_store ON saved_report (store_id);
CREATE TABLE attribution (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    subject_kind VARCHAR(8) NOT NULL,
    subject_id VARCHAR(32) NOT NULL,
    first_source VARCHAR(100) NOT NULL,
    first_medium VARCHAR(100) NOT NULL,
    first_campaign VARCHAR(100) NOT NULL,
    last_source VARCHAR(100) NOT NULL,
    last_medium VARCHAR(100) NOT NULL,
    last_campaign VARCHAR(100) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX attribution_subject ON attribution (subject_kind, subject_id);
CREATE INDEX attribution_store ON attribution (store_id);
