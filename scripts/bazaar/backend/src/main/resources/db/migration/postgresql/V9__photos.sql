-- Pictures, as the image file's bytes: every instance of the server reads
-- the same pictures, and a backup of the database is a backup of them.
-- Written per engine because each names its binary type differently.
CREATE TABLE photo (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(32) NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    data BYTEA NOT NULL
);
CREATE INDEX photo_owner ON photo (owner_id);
