-- The commission rate a counter sale earned when it was made, so changing a
-- rate later does not rewrite what was already earned. -1 marks a sale made
-- before rates were kept per sale: the report uses the current rate for it.
ALTER TABLE store_order ADD COLUMN commission_bp BIGINT NOT NULL DEFAULT -1;
