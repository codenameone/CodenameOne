-- Restaurant and dish pictures, as the image file's bytes, for the same reason the
-- onboarding documents are kept in the database (see V3): every instance of
-- the server reads the same pictures, and a backup of the database is a backup
-- of them. Written per engine because each names its binary type differently.
CREATE TABLE photo (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    restaurant_id VARCHAR(32) NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    data LONGBLOB NOT NULL
);
CREATE INDEX photo_restaurant ON photo (restaurant_id);
