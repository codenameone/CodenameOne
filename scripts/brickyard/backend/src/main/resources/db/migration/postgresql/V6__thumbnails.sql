-- An experience's tile picture, captured from the renderer when it is published.
CREATE TABLE experience_thumbnail (
    experience_id VARCHAR(32) NOT NULL PRIMARY KEY,
    image BYTEA NOT NULL,
    updated_at BIGINT NOT NULL
);
