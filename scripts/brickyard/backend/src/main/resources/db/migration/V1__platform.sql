-- The tables every database shares. Portable SQL only: no generated keys, no
-- native boolean (a yes/no is a SMALLINT 0/1), times are epoch milliseconds.
-- The tables holding large values differ per database and live in V2 beside
-- this directory.

CREATE TABLE account (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    display_name VARCHAR(40) NOT NULL,
    birth_year INTEGER NOT NULL,
    bricks BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE friend (
    id VARCHAR(400) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    friend VARCHAR(190) NOT NULL
);
CREATE INDEX friend_username ON friend (username);

CREATE TABLE experience (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    title VARCHAR(80) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    genre VARCHAR(32) NOT NULL,
    max_players INTEGER NOT NULL,
    access_level VARCHAR(16) NOT NULL,
    age_rating VARCHAR(8) NOT NULL,
    published_version INTEGER NOT NULL,
    latest_version INTEGER NOT NULL,
    visits BIGINT NOT NULL,
    favourites BIGINT NOT NULL,
    up_votes BIGINT NOT NULL,
    down_votes BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX experience_owner ON experience (owner);

CREATE TABLE reaction (
    id VARCHAR(240) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    favourite SMALLINT NOT NULL,
    vote INTEGER NOT NULL
);

CREATE TABLE stat_bound (
    id VARCHAR(100) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    stat VARCHAR(64) NOT NULL,
    max_per_minute BIGINT NOT NULL
);

CREATE TABLE leaderboard_entry (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    stat VARCHAR(64) NOT NULL,
    user_id VARCHAR(190) NOT NULL,
    stat_value BIGINT NOT NULL,
    day_start BIGINT NOT NULL
);
CREATE INDEX leaderboard_entry_stat ON leaderboard_entry (experience_id, stat, day_start);

CREATE TABLE score_write (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    stat VARCHAR(64) NOT NULL,
    user_id VARCHAR(190) NOT NULL,
    delta BIGINT NOT NULL,
    written_at BIGINT NOT NULL
);
CREATE INDEX score_write_user ON score_write (experience_id, stat, user_id, written_at);

CREATE TABLE cheat_flag (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    stat VARCHAR(64) NOT NULL,
    user_id VARCHAR(190) NOT NULL,
    delta BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE badge_definition (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    name VARCHAR(60) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE badge_award (
    id VARCHAR(240) NOT NULL PRIMARY KEY,
    badge_id VARCHAR(32) NOT NULL,
    experience_id VARCHAR(32) NOT NULL,
    user_id VARCHAR(190) NOT NULL,
    awarded_at BIGINT NOT NULL
);
CREATE INDEX badge_award_user ON badge_award (user_id);
