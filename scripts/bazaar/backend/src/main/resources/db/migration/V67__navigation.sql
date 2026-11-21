-- Wave C: nested navigation menus and the 301 table.
CREATE TABLE menu_link (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    menu VARCHAR(16) NOT NULL,
    parent_id VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    url VARCHAR(500) NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX menu_link_store_menu ON menu_link (store_id, menu);
CREATE TABLE url_redirect (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    from_path VARCHAR(190) NOT NULL,
    to_path VARCHAR(500) NOT NULL,
    hits BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX url_redirect_store_from_key ON url_redirect (store_id, from_path);
