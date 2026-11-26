-- Wave B: subscriptions: selling plans and customer subscriptions.
CREATE TABLE selling_plan (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    cadence INTEGER NOT NULL,
    discount_bps INTEGER NOT NULL,
    active SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE TABLE subscription (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    plan_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL,
    discount_bps INTEGER NOT NULL,
    unit VARCHAR(16) NOT NULL,
    cadence INTEGER NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    email VARCHAR(190) NOT NULL,
    payment_method_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    anchor_at BIGINT NOT NULL,
    cycles INTEGER NOT NULL,
    due_at BIGINT NOT NULL,
    next_attempt_at BIGINT NOT NULL,
    failures INTEGER NOT NULL,
    last_order_id VARCHAR(32) NOT NULL,
    last_error VARCHAR(200) NOT NULL,
    ship_name VARCHAR(120) NOT NULL,
    ship_line1 VARCHAR(200) NOT NULL,
    ship_city VARCHAR(120) NOT NULL,
    ship_region VARCHAR(80) NOT NULL,
    ship_postal VARCHAR(20) NOT NULL,
    ship_country VARCHAR(2) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX selling_plan_store_id ON selling_plan (store_id);
CREATE INDEX selling_plan_product_id ON selling_plan (product_id);
CREATE INDEX subscription_store_id_username ON subscription (store_id, username);
CREATE INDEX subscription_status_next ON subscription (status, next_attempt_at);
CREATE INDEX subscription_store_id_due_at ON subscription (store_id, due_at);
