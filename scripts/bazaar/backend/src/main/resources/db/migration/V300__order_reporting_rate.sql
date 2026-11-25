-- Every order records, at the time of sale, the rate from its store's
-- currency into the platform's reporting currency, so the platform's GMV adds
-- amounts in one currency and an old order keeps the rate it was sold at.
ALTER TABLE store_order ADD COLUMN reporting_currency VARCHAR(3) NOT NULL DEFAULT '';
ALTER TABLE store_order ADD COLUMN reporting_rate_micros BIGINT NOT NULL DEFAULT 0;
