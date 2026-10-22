-- The Forge's packs and files. The large text columns are LONGTEXT here, the
-- type the ORM maps a large String to on MySQL and validates against.
CREATE TABLE forge_pack (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    name VARCHAR(60) NOT NULL,
    author VARCHAR(60) NOT NULL,
    description VARCHAR(1200) NOT NULL,
    status VARCHAR(12) NOT NULL,
    official SMALLINT NOT NULL,
    version INTEGER NOT NULL,
    hash VARCHAR(16) NOT NULL,
    draft LONGTEXT NOT NULL,
    forked_from VARCHAR(40) NOT NULL,
    plays INTEGER NOT NULL,
    rating_sum INTEGER NOT NULL,
    rating_count INTEGER NOT NULL,
    reports INTEGER NOT NULL,
    missions INTEGER NOT NULL,
    cards INTEGER NOT NULL,
    pending_assets SMALLINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE forge_pack_version (
    id VARCHAR(60) NOT NULL PRIMARY KEY,
    pack_id VARCHAR(40) NOT NULL,
    version INTEGER NOT NULL,
    hash VARCHAR(16) NOT NULL,
    content LONGTEXT NOT NULL,
    published_at BIGINT NOT NULL
);

CREATE TABLE forge_asset (
    sha256 VARCHAR(64) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    name VARCHAR(80) NOT NULL,
    kind VARCHAR(8) NOT NULL,
    bytes INTEGER NOT NULL,
    status VARCHAR(10) NOT NULL,
    animations VARCHAR(2000) NOT NULL,
    data LONGTEXT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE mission_stars (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    pack_id VARCHAR(40) NOT NULL,
    mission_id VARCHAR(32) NOT NULL,
    stars INTEGER NOT NULL,
    paid_stars INTEGER NOT NULL,
    plays INTEGER NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE forge_rating (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    pack_id VARCHAR(40) NOT NULL,
    stars INTEGER NOT NULL
);

CREATE TABLE forge_earning (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    day BIGINT NOT NULL,
    coins INTEGER NOT NULL
);
CREATE INDEX forge_pack_owner ON forge_pack (owner, updated_at);
CREATE INDEX forge_pack_status ON forge_pack (status, official, updated_at);
CREATE INDEX forge_pack_version_pack ON forge_pack_version (pack_id, version);
CREATE INDEX forge_asset_status ON forge_asset (status, created_at);
CREATE INDEX mission_stars_player ON mission_stars (username, pack_id);
CREATE INDEX forge_rating_pack ON forge_rating (pack_id);
