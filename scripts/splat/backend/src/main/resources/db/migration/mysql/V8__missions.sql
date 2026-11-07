-- Player-built missions. The body (LONGTEXT, what the ORM maps a large String to) is the mission's JSON, a few kilobytes.
CREATE TABLE mission_record (
    code VARCHAR(16) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    name VARCHAR(40) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    body LONGTEXT NOT NULL,
    created_at BIGINT NOT NULL,
    plays INTEGER NOT NULL
);

CREATE INDEX mission_record_owner ON mission_record (owner, created_at);
