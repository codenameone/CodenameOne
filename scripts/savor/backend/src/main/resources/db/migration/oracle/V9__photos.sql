-- Restaurant and dish pictures, as the image file's bytes, for the same reason the
-- onboarding documents are kept in the database (see V3): every instance of
-- the server reads the same pictures, and a backup of the database is a backup
-- of them. Written per engine because each names its binary type differently.
CREATE TABLE photo (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    restaurant_id VARCHAR2(32 CHAR),
    content_type VARCHAR2(32 CHAR),
    created_at NUMBER(19) NOT NULL,
    data BLOB NOT NULL
);
CREATE INDEX photo_restaurant ON photo (restaurant_id);
