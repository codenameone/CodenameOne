-- A BUNDLE product is made of other variants, each a quantity of the bundle.
CREATE TABLE bundle_component (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX bundle_component_product ON bundle_component (product_id);
CREATE INDEX bundle_component_store ON bundle_component (store_id);

-- When a product appears on a sales channel: ONLINE (the storefront), POS,
-- or a sales_channel id. visible is what the scheduler last set from
-- publish_at and unpublish_at (0 for no bound); a product with no row for a
-- channel follows its status alone.
CREATE TABLE channel_schedule (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    publish_at BIGINT NOT NULL,
    unpublish_at BIGINT NOT NULL,
    visible SMALLINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX channel_schedule_product ON channel_schedule (product_id, channel);
CREATE INDEX channel_schedule_store ON channel_schedule (store_id);
