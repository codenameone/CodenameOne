-- App slots: third-party snippets an owner places on the HTML storefront.

-- One snippet: an external script, a sandboxed HTML widget, or both, at one
-- named place, with the hosts it may reach and the consent it waits for.
CREATE TABLE app_slot (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    slot VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    script_url VARCHAR(500) NOT NULL,
    widget_html TEXT NOT NULL,
    origins VARCHAR(1000) NOT NULL,
    category VARCHAR(16) NOT NULL,
    enabled SMALLINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX app_slot_store_id ON app_slot (store_id);

-- Bumped by every change to a store's slots, so a public page's ETag changes
-- with its policy even when no byte of its HTML does (and on a delete, which
-- leaves no row to carry a timestamp).
CREATE TABLE app_slot_version (
    store_id VARCHAR(32) NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL
);
