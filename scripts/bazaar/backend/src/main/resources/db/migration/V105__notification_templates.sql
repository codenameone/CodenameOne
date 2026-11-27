-- A store's own wording of a shopper notification text, per language. With
-- no row the built-in text is used unchanged.
CREATE TABLE notification_template (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    language VARCHAR(8) NOT NULL,
    text_key VARCHAR(64) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX notification_template_key ON notification_template (store_id, language, text_key);
