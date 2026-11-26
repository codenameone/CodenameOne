-- Shipping profiles: groups of products with zones and rates of their own
-- ("Oversized", "Fragile"). A zone belongs to one profile (empty = the
-- store's general profile); a product is in at most one profile, and in the
-- general one while it has no row here.
CREATE TABLE shipping_profile (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX shipping_profile_store_position ON shipping_profile (store_id, position);
CREATE TABLE shipping_profile_product (
    product_id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    profile_id VARCHAR(32) NOT NULL
);
CREATE INDEX shipping_profile_product_store ON shipping_profile_product (store_id, profile_id);
ALTER TABLE shipping_zone ADD COLUMN profile_id VARCHAR(32) NOT NULL DEFAULT '';
