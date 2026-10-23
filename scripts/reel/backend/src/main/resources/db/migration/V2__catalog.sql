-- Every script here is written once for SQLite, PostgreSQL and MySQL: no
-- generated keys, no native boolean or timestamp. A yes/no is a SMALLINT 0/1,
-- a time is milliseconds since the epoch in a BIGINT, and a list is a
-- comma-separated VARCHAR.

-- A genre, or an academy subject.
CREATE TABLE genre (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    position INTEGER NOT NULL
);

-- A movie, a series, a course or a creator's video, with its listing and moderation state.
CREATE TABLE title (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    kind VARCHAR(16) NOT NULL,
    name VARCHAR(200) NOT NULL,
    synopsis TEXT NOT NULL,
    year INTEGER NOT NULL,
    rating VARCHAR(8) NOT NULL,
    genres VARCHAR(255) NOT NULL,
    tags VARCHAR(255) NOT NULL,
    creator VARCHAR(190) NOT NULL,
    access VARCHAR(16) NOT NULL,
    rental_price_cents BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    moderation_note VARCHAR(255) NOT NULL,
    poster_url VARCHAR(255) NOT NULL,
    backdrop_url VARCHAR(255) NOT NULL,
    certificate SMALLINT NOT NULL,
    pass_percent INTEGER NOT NULL,
    featured SMALLINT NOT NULL,
    age_restricted SMALLINT NOT NULL,
    created_at BIGINT NOT NULL,
    published_at BIGINT NOT NULL,
    announced_videos INTEGER NOT NULL,
    rating_sum BIGINT NOT NULL,
    rating_count INTEGER NOT NULL,
    view_count BIGINT NOT NULL
);
CREATE INDEX title_status_published_at ON title (status, published_at);
CREATE INDEX title_creator ON title (creator);

-- One playable file of a title: the feature, an episode or lesson, or the trailer.
CREATE TABLE video (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    title_id VARCHAR(32) NOT NULL,
    role VARCHAR(16) NOT NULL,
    season INTEGER NOT NULL,
    number INTEGER NOT NULL,
    name VARCHAR(200) NOT NULL,
    synopsis TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    progress_percent INTEGER NOT NULL,
    error VARCHAR(255) NOT NULL,
    duration_seconds INTEGER NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    renditions VARCHAR(255) NOT NULL,
    schemes VARCHAR(64) NOT NULL,
    thumbnail_url VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX video_title_id ON video (title_id);

-- A named point in a video.
CREATE TABLE chapter (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    video_id VARCHAR(32) NOT NULL,
    start_seconds INTEGER NOT NULL,
    title VARCHAR(120) NOT NULL
);
CREATE INDEX chapter_video_id ON chapter (video_id);

-- A pause point in a video: a quiz, a poll, a branch or a note. `options` are one per line.
CREATE TABLE interaction (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    video_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    at_seconds INTEGER NOT NULL,
    prompt VARCHAR(500) NOT NULL,
    options TEXT NOT NULL,
    answers VARCHAR(64) NOT NULL,
    explanation VARCHAR(500) NOT NULL,
    required SMALLINT NOT NULL,
    branch_targets VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX interaction_video_id ON interaction (video_id);

-- The content key a video is encrypted with. Never written to the media directory.
CREATE TABLE video_key (
    video_id VARCHAR(32) NOT NULL PRIMARY KEY,
    key_id VARCHAR(32) NOT NULL,
    key_hex VARCHAR(32) NOT NULL,
    iv_hex VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);

-- A perceptual hash of one sampled frame, for matching against the blocklist.
CREATE TABLE frame_hash (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    video_id VARCHAR(32) NOT NULL,
    hash VARCHAR(16) NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX frame_hash_hash ON frame_hash (hash);
CREATE INDEX frame_hash_video_id ON frame_hash (video_id);
