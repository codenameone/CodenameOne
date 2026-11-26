-- Exchange rates from a feed beside the merchant's own. A market chooses
-- which it is priced at (fx_source: manual or ecb); the feed never
-- overwrites the manual rate, and an order keeps where its rate came from.
ALTER TABLE market ADD COLUMN fx_source VARCHAR(16) NOT NULL DEFAULT 'manual';
ALTER TABLE fx_rate ADD COLUMN feed_micros BIGINT NOT NULL DEFAULT 0;
ALTER TABLE fx_rate ADD COLUMN feed_source VARCHAR(16) NOT NULL DEFAULT '';
ALTER TABLE fx_rate ADD COLUMN feed_at BIGINT NOT NULL DEFAULT 0;
ALTER TABLE store_order ADD COLUMN fx_source VARCHAR(16) NOT NULL DEFAULT '';
