-- The shopper's language (a StoreText language code, or empty when not
-- known): on the store's customer record, the cart and the order, so the
-- mail about each goes out in the language it was placed in.
ALTER TABLE customer ADD COLUMN language VARCHAR(8) NOT NULL DEFAULT '';
ALTER TABLE cart ADD COLUMN language VARCHAR(8) NOT NULL DEFAULT '';
ALTER TABLE store_order ADD COLUMN language VARCHAR(8) NOT NULL DEFAULT '';
