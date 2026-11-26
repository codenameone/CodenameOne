-- Wave C: combined listings, separate products shown as one with a colour each.
CREATE TABLE combined_listing (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX combined_listing_store ON combined_listing (store_id);
CREATE TABLE combined_listing_member (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    listing_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    color VARCHAR(60) NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX combined_listing_member_listing ON combined_listing_member (listing_id);
CREATE UNIQUE INDEX combined_listing_member_product ON combined_listing_member (product_id);
