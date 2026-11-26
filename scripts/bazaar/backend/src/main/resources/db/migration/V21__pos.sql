-- Register PIN lockout: wrong PINs in a row, and until when the PIN is refused.
ALTER TABLE store_staff ADD COLUMN pin_failures INTEGER NOT NULL DEFAULT 0;
ALTER TABLE store_staff ADD COLUMN pin_locked_until BIGINT NOT NULL DEFAULT 0;

-- Where a register taxes its sales: a counter sale has no shipping address.
ALTER TABLE register ADD COLUMN tax_country VARCHAR(2) NOT NULL DEFAULT '';
ALTER TABLE register ADD COLUMN tax_region VARCHAR(80) NOT NULL DEFAULT '';
