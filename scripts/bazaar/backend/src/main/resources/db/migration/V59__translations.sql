-- Wave B: per-locale translations of product, collection and page fields.
CREATE TABLE translation (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    ref_id VARCHAR(32) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    field VARCHAR(32) NOT NULL,
    body TEXT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX translation_key ON translation (store_id, kind, ref_id, locale, field);
CREATE INDEX translation_store_locale ON translation (store_id, locale);
