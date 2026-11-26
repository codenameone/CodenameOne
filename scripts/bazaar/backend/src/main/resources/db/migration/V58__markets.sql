-- Wave B: markets (countries, currency, rounding, adjustment), per-market prices, FX rates, customs codes and duty rates.
CREATE TABLE market (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    code VARCHAR(16) NOT NULL,
    name VARCHAR(80) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    countries VARCHAR(400) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    rounding VARCHAR(16) NOT NULL,
    adjust_bps INTEGER NOT NULL,
    active SMALLINT NOT NULL,
    position INTEGER NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX market_store_code_key ON market (store_id, code);
CREATE TABLE market_price (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    market_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    price_cents BIGINT NOT NULL
);
CREATE UNIQUE INDEX market_price_market_variant_key ON market_price (market_id, variant_id);
CREATE INDEX market_price_store_id ON market_price (store_id);
CREATE TABLE fx_rate (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    rate_micros BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX fx_rate_store_currency_key ON fx_rate (store_id, currency);
CREATE TABLE customs_code (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    hs_code VARCHAR(12) NOT NULL
);
CREATE UNIQUE INDEX customs_code_product_key ON customs_code (product_id);
CREATE INDEX customs_code_store_id ON customs_code (store_id);
CREATE TABLE duty_rate (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    country VARCHAR(2) NOT NULL,
    hs_prefix VARCHAR(12) NOT NULL,
    rate_bps INTEGER NOT NULL
);
CREATE INDEX duty_rate_store_country ON duty_rate (store_id, country);
