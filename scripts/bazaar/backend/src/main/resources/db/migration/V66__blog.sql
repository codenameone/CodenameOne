-- Wave C: a blog per store.
CREATE TABLE blog_post (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    handle VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(120) NOT NULL,
    tags VARCHAR(400) NOT NULL,
    excerpt VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    published SMALLINT NOT NULL,
    published_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX blog_post_store_handle_key ON blog_post (store_id, handle);
CREATE INDEX blog_post_store_published ON blog_post (store_id, published_at);
