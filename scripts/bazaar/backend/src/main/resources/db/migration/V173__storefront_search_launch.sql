-- Storefront search tuning and the pre-launch password: a store's synonym
-- groups and password, and a product's boost in search ranking.
ALTER TABLE store ADD COLUMN search_synonyms VARCHAR(2000) DEFAULT '' NOT NULL;
ALTER TABLE store ADD COLUMN storefront_password VARCHAR(100) DEFAULT '' NOT NULL;
ALTER TABLE product ADD COLUMN search_boost INTEGER DEFAULT 0 NOT NULL;
