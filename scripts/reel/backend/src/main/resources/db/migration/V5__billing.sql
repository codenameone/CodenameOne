-- Every script here is written once for SQLite, PostgreSQL and MySQL: no
-- generated keys, no native boolean or timestamp. A yes/no is a SMALLINT 0/1,
-- a time is milliseconds since the epoch in a BIGINT, and a list is a
-- comma-separated VARCHAR.

-- A subscription plan.
CREATE TABLE plan (
    code VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    description VARCHAR(255) NOT NULL,
    price_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    billing_interval VARCHAR(8) NOT NULL,
    max_streams INTEGER NOT NULL,
    max_quality VARCHAR(8) NOT NULL,
    ad_supported SMALLINT NOT NULL,
    downloads SMALLINT NOT NULL,
    trial_days INTEGER NOT NULL,
    active SMALLINT NOT NULL,
    position INTEGER NOT NULL
);

-- An account's subscription. One row per account, kept after it ends.
CREATE TABLE subscription (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    plan_code VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at BIGINT NOT NULL,
    period_ends_at BIGINT NOT NULL,
    cancel_at_period_end SMALLINT NOT NULL,
    pending_plan_code VARCHAR(32) NOT NULL,
    coupon_code VARCHAR(32) NOT NULL,
    coupon_periods_left INTEGER NOT NULL,
    payment_method_id VARCHAR(32) NOT NULL,
    failed_attempts INTEGER NOT NULL,
    store VARCHAR(16) NOT NULL,
    store_ref VARCHAR(190) NOT NULL,
    updated_at BIGINT NOT NULL,
    next_retry_at BIGINT NOT NULL
);
CREATE INDEX subscription_status_period_ends_at ON subscription (status, period_ends_at);

-- That an account used a coupon: id is code:username.
CREATE TABLE coupon_redemption (
    id VARCHAR(230) NOT NULL PRIMARY KEY,
    redeemed_at BIGINT NOT NULL
);

-- A percent off for a number of billing periods.
CREATE TABLE coupon (
    code VARCHAR(32) NOT NULL PRIMARY KEY,
    percent_off INTEGER NOT NULL,
    periods INTEGER NOT NULL,
    max_redemptions INTEGER NOT NULL,
    redemptions INTEGER NOT NULL,
    expires_at BIGINT NOT NULL,
    active SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);

-- A billing record: a period, a rental, a proration, a refund.
CREATE TABLE invoice (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    provider_ref VARCHAR(64) NOT NULL,
    title_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX invoice_username_created_at ON invoice (username, created_at);
CREATE INDEX invoice_created_at ON invoice (created_at);

-- A title an account may play until the rental expires.
CREATE TABLE rental (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    title_id VARCHAR(32) NOT NULL,
    price_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    started_at BIGINT NOT NULL,
    invoice_id VARCHAR(32) NOT NULL
);
CREATE INDEX rental_username ON rental (username);

CREATE TABLE payment_customer (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    reference VARCHAR(64) NOT NULL
);

CREATE TABLE payment_method (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    brand VARCHAR(24) NOT NULL,
    last4 VARCHAR(4) NOT NULL,
    exp_month INTEGER NOT NULL,
    exp_year INTEGER NOT NULL,
    token VARCHAR(64) NOT NULL,
    preferred SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX payment_method_username_created_at ON payment_method (username, created_at);

CREATE TABLE payment_setup (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    reference VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    completed SMALLINT NOT NULL
);
