-- Wave C: the online store's theme (live and draft) and its pages and policies.
CREATE TABLE store_theme (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    status VARCHAR(8) NOT NULL,
    accent VARCHAR(7) NOT NULL,
    palette VARCHAR(16) NOT NULL,
    font VARCHAR(16) NOT NULL,
    radius VARCHAR(16) NOT NULL,
    logo_photo_id VARCHAR(32) NOT NULL,
    sections VARCHAR(200) NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX store_theme_store_status_key ON store_theme (store_id, status);
CREATE TABLE content_page (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    kind VARCHAR(8) NOT NULL,
    handle VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    published SMALLINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX content_page_store_kind_handle_key ON content_page (store_id, kind, handle);
