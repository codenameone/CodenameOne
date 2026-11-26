-- A store's own hostname: requests whose Host names it are served that
-- store at the root paths. NULL for a store without one, so the unique
-- index holds only the domains actually set.
ALTER TABLE store ADD COLUMN custom_domain VARCHAR(253);
CREATE UNIQUE INDEX ux_store_custom_domain ON store (custom_domain);
