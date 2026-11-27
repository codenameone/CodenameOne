-- Where an online order was placed: WEB (the storefront) or APP (the
-- shopping app or the API). Blank for orders placed before it was kept.
ALTER TABLE store_order ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT '';
