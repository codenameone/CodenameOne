-- Wave C: an order charged in the shopper's market currency keeps that
-- currency, the amount charged in it and the rate it was converted at; every
-- other money column stays in the store's (settlement) currency.
ALTER TABLE store_order ADD COLUMN presentment_currency VARCHAR(3) NOT NULL DEFAULT '';
ALTER TABLE store_order ADD COLUMN presentment_total_cents BIGINT NOT NULL DEFAULT 0;
ALTER TABLE store_order ADD COLUMN fx_rate_micros BIGINT NOT NULL DEFAULT 0;
ALTER TABLE refund ADD COLUMN presentment_cents BIGINT NOT NULL DEFAULT 0;
