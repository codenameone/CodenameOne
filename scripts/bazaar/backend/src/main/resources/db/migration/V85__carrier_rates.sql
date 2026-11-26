-- Carrier-calculated shipping: a CARRIER rate's markup on the carrier's
-- price (hundredths of a percent) and the price it falls back to when the
-- carrier cannot answer (0 = none).
ALTER TABLE shipping_rate ADD COLUMN markup_bps INTEGER NOT NULL DEFAULT 0;
ALTER TABLE shipping_rate ADD COLUMN fallback_cents BIGINT NOT NULL DEFAULT 0;
