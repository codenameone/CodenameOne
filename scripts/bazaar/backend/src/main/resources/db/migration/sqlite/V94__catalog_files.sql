-- Files of the catalog that are not pictures: a product video (mp4), a 3D
-- model (glb) and a digital download. Kept as bytes like the photos, and
-- written per engine for the binary type. Only VIDEO and MODEL are served
-- publicly; a DOWNLOAD is served only through a signed, counted grant.
CREATE TABLE catalog_file (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    name VARCHAR(200) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    data BLOB NOT NULL
);
CREATE INDEX catalog_file_store ON catalog_file (store_id);
