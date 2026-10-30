-- A replay is a few kilobytes of text, more than a portable VARCHAR holds, and
-- the large text type is spelled differently here than on the other databases:
-- LONGTEXT, which is what the ORM maps a large String to and validates against.
CREATE TABLE race_record (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    level VARCHAR(160) NOT NULL,
    players VARCHAR(1200) NOT NULL,
    names VARCHAR(300) NOT NULL,
    places VARCHAR(40) NOT NULL,
    scores VARCHAR(120) NOT NULL,
    trophies VARCHAR(120) NOT NULL,
    runners INTEGER NOT NULL,
    humans INTEGER NOT NULL,
    ticks INTEGER NOT NULL,
    ended_at BIGINT NOT NULL,
    rules_version VARCHAR(16) NOT NULL,
    replay LONGTEXT NOT NULL
);
CREATE INDEX race_record_ended_at ON race_record (ended_at);

-- One row per player per race, so a history is an indexed lookup.
CREATE TABLE race_entry (
    id VARCHAR(230) NOT NULL PRIMARY KEY,
    race_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    level VARCHAR(160) NOT NULL,
    place INTEGER NOT NULL,
    runners INTEGER NOT NULL,
    score INTEGER NOT NULL,
    trophy_change INTEGER NOT NULL,
    ticks INTEGER NOT NULL,
    ended_at BIGINT NOT NULL
);
CREATE INDEX race_entry_player ON race_entry (username, ended_at);

-- A player's best verified run per level per board; the replay proves it.
CREATE TABLE score (
    id VARCHAR(400) NOT NULL PRIMARY KEY,
    board VARCHAR(16) NOT NULL,
    level VARCHAR(160) NOT NULL,
    username VARCHAR(190) NOT NULL,
    display_name VARCHAR(40) NOT NULL,
    score INTEGER NOT NULL,
    distance INTEGER NOT NULL,
    coins INTEGER NOT NULL,
    stars INTEGER NOT NULL,
    seed BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    replay LONGTEXT NOT NULL
);
CREATE INDEX score_board ON score (board, level, score);
CREATE INDEX score_player ON score (username);

CREATE TABLE crew (
    code VARCHAR(16) NOT NULL PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    owner VARCHAR(190) NOT NULL,
    members INTEGER NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE crew_member (
    id VARCHAR(210) NOT NULL PRIMARY KEY,
    code VARCHAR(16) NOT NULL,
    username VARCHAR(190) NOT NULL,
    joined_at BIGINT NOT NULL
);
CREATE INDEX crew_member_code ON crew_member (code, joined_at);
CREATE INDEX crew_member_player ON crew_member (username, joined_at);
