-- Tax rules beyond a rate per region.
--
-- A product's tax category picks the rate of its kind of goods (standard,
-- reduced, super-reduced, zero or exempt) where the destination has one.
ALTER TABLE product ADD COLUMN tax_category VARCHAR(16) NOT NULL DEFAULT 'standard';

-- The rate an order line was taxed at, for the VAT reports. -1 marks a line
-- sold before rates were kept per line.
ALTER TABLE order_line ADD COLUMN tax_bp BIGINT NOT NULL DEFAULT -1;

-- A store's tax engine: whether EU destinations are taxed from the EU VAT
-- table (the One-Stop Shop way), the country the store is established in,
-- and which US sales-tax provider prices US destinations ('' for none).
CREATE TABLE tax_settings (
    store_id VARCHAR(32) NOT NULL PRIMARY KEY,
    eu_vat SMALLINT NOT NULL,
    home_country VARCHAR(2) NOT NULL,
    us_provider VARCHAR(32) NOT NULL,
    updated_at BIGINT NOT NULL
);

-- A rate that replaces the usual one for a product, or for every product in
-- a collection, in a country or one region of it.
CREATE TABLE tax_override (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    target_kind VARCHAR(16) NOT NULL,
    target_id VARCHAR(32) NOT NULL,
    country VARCHAR(2) NOT NULL,
    region VARCHAR(80) NOT NULL,
    basis_points INTEGER NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX tax_override_store_id ON tax_override (store_id);

-- The EU VAT rate of each member state for each tax category. Seeded by the
-- next migration; the platform administrator keeps it current.
CREATE TABLE eu_vat_rate (
    country VARCHAR(2) NOT NULL,
    category VARCHAR(16) NOT NULL,
    basis_points INTEGER NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (country, category)
);
