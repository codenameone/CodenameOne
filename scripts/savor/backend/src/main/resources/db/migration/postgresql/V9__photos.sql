-- Restaurant and dish pictures, as base64 text, for the same reason the
-- onboarding documents are kept in the database (see V3): every instance of
-- the server reads the same pictures, and a backup of the database is a backup
-- of them. Written per engine for the same reason too.
CREATE TABLE photo (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    restaurant_id VARCHAR(32) NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    data TEXT NOT NULL
);
CREATE INDEX photo_restaurant ON photo (restaurant_id);
