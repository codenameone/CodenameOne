-- Order routing: the ordered rules that pick the location fulfilling each
-- line of an online order, the store's split and fulfilment service
-- settings, and the orders handed to a fulfilment service (3PL). A store
-- with no rows here routes exactly as before.
CREATE TABLE routing_rule (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    location_ids VARCHAR(1000) NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX routing_rule_store_position ON routing_rule (store_id, position);
CREATE TABLE routing_setting (
    store_id VARCHAR(32) NOT NULL PRIMARY KEY,
    allow_split SMALLINT NOT NULL,
    service VARCHAR(32) NOT NULL,
    service_location_id VARCHAR(32) NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE TABLE fulfilment_service_order (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    service VARCHAR(32) NOT NULL,
    external_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    carrier VARCHAR(32) NOT NULL,
    tracking_number VARCHAR(64) NOT NULL,
    fulfillment_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX fulfilment_service_order_order ON fulfilment_service_order (order_id, location_id);
CREATE INDEX fulfilment_service_order_store ON fulfilment_service_order (store_id, created_at);
CREATE INDEX fulfilment_service_order_status ON fulfilment_service_order (status);
