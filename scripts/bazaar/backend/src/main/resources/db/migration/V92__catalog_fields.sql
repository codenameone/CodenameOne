-- Catalog depth on the rows that already exist.
--
-- variant: a digital variant needs no shipping; customs data (HS code and
-- country of origin) for international labels; a unit price measure, such as
-- 750 ml priced per 1 l (unit_quantity in thousandths of unit_name).
ALTER TABLE variant ADD COLUMN digital SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE variant ADD COLUMN hs_code VARCHAR(16) NOT NULL DEFAULT '';
ALTER TABLE variant ADD COLUMN origin_country VARCHAR(2) NOT NULL DEFAULT '';
ALTER TABLE variant ADD COLUMN unit_quantity BIGINT NOT NULL DEFAULT 0;
ALTER TABLE variant ADD COLUMN unit_name VARCHAR(8) NOT NULL DEFAULT '';
ALTER TABLE variant ADD COLUMN unit_reference INTEGER NOT NULL DEFAULT 0;
ALTER TABLE variant ADD COLUMN unit_reference_name VARCHAR(8) NOT NULL DEFAULT '';
-- product: STANDARD or BUNDLE, and its Google product taxonomy category id
-- (0 for none).
ALTER TABLE product ADD COLUMN kind VARCHAR(16) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE product ADD COLUMN category_id INTEGER NOT NULL DEFAULT 0;
-- product_photo: the gallery holds IMAGE, VIDEO and MODEL media. An uploaded
-- video or model names a catalog_file in photo_id; a linked video has an
-- https url instead.
ALTER TABLE product_photo ADD COLUMN kind VARCHAR(8) NOT NULL DEFAULT 'IMAGE';
ALTER TABLE product_photo ADD COLUMN url VARCHAR(500) NOT NULL DEFAULT '';
