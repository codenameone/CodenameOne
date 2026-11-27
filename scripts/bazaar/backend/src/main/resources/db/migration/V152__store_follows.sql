-- The stores a shopper follows, for the app's Following feed.
CREATE TABLE store_follow (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX store_follow_username_store_id_key ON store_follow (username, store_id);
