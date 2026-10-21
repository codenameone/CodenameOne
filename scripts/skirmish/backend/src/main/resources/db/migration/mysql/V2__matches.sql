-- A replay is a few kilobytes of text, more than a portable VARCHAR holds, and
-- the large text type is spelled differently here than on the other databases.
CREATE TABLE match_record (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    player0 VARCHAR(190) NOT NULL,
    player1 VARCHAR(190) NOT NULL,
    name0 VARCHAR(40) NOT NULL,
    name1 VARCHAR(40) NOT NULL,
    result INTEGER NOT NULL,
    crowns0 INTEGER NOT NULL,
    crowns1 INTEGER NOT NULL,
    trophies0 INTEGER NOT NULL,
    trophies1 INTEGER NOT NULL,
    ticks INTEGER NOT NULL,
    ended_at BIGINT NOT NULL,
    rules_version VARCHAR(16) NOT NULL,
    replay MEDIUMTEXT NOT NULL
);
CREATE INDEX match_record_player0 ON match_record (player0, ended_at);
CREATE INDEX match_record_player1 ON match_record (player1, ended_at);
CREATE INDEX match_record_ended_at ON match_record (ended_at);
