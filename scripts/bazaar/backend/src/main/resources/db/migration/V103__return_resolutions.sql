-- What a shopper asked for in return for the goods: the money back, store
-- credit, or another variant; and the fees the store's rules took.
ALTER TABLE return_request ADD COLUMN resolution VARCHAR(16) NOT NULL DEFAULT 'REFUND';
ALTER TABLE return_request ADD COLUMN exchange_variant_id VARCHAR(32) NOT NULL DEFAULT '';
ALTER TABLE return_request ADD COLUMN fee_cents BIGINT NOT NULL DEFAULT 0;
ALTER TABLE return_request ADD COLUMN replacement_order_id VARCHAR(32) NOT NULL DEFAULT '';
