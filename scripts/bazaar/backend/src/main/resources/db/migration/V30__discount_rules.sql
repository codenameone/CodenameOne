-- Wave A: discount kinds beyond a percentage or an amount (tiered, volume,
-- bundle), who may use a discount (a loyalty tier or a club), and the
-- combination classes that decide what stacks with what. Every column has a
-- default that keeps an existing discount exactly as it was.
ALTER TABLE discount ADD COLUMN discount_class VARCHAR(16) NOT NULL DEFAULT '';
ALTER TABLE discount ADD COLUMN combines_product SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE discount ADD COLUMN combines_order SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE discount ADD COLUMN combines_shipping SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE discount ADD COLUMN eligibility VARCHAR(80) NOT NULL DEFAULT '';
ALTER TABLE discount ADD COLUMN rules VARCHAR(2000) NOT NULL DEFAULT '';
