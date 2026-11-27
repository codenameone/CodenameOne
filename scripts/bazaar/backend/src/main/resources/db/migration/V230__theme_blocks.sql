-- Theme blocks: the ordered blocks of the home page's "blocks" section (heading,
-- text, image, product, collection, button), one a line, kind/text/reference split
-- by tabs. Kept on the theme row so a draft's blocks publish with the rest of it.
ALTER TABLE store_theme ADD COLUMN blocks VARCHAR(4000) NOT NULL DEFAULT '';
