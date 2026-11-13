-- Inviting friends and promoting the game: invite codes, who joined through
-- whom, the pushes operators sent and the home banner. Portable, like V4.

CREATE TABLE referral_code (
    code VARCHAR(16) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE referral (
    invitee VARCHAR(190) NOT NULL PRIMARY KEY,
    inviter VARCHAR(190) NOT NULL,
    code VARCHAR(16) NOT NULL,
    campaign VARCHAR(40) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    match_type VARCHAR(20) NOT NULL,
    state VARCHAR(10) NOT NULL,
    rewarded SMALLINT NOT NULL,
    joined_at BIGINT NOT NULL,
    played_at BIGINT NOT NULL
);

CREATE TABLE broadcast (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    segment VARCHAR(10) NOT NULL,
    days INTEGER NOT NULL,
    title VARCHAR(60) NOT NULL,
    body VARCHAR(200) NOT NULL,
    screen VARCHAR(20) NOT NULL,
    campaign VARCHAR(40) NOT NULL,
    targets INTEGER NOT NULL,
    sent_by VARCHAR(190) NOT NULL,
    sent_at BIGINT NOT NULL
);

CREATE TABLE spotlight (
    id VARCHAR(10) NOT NULL PRIMARY KEY,
    message VARCHAR(140) NOT NULL,
    pack_id VARCHAR(40) NOT NULL,
    screen VARCHAR(20) NOT NULL,
    updated_by VARCHAR(190) NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX referral_by_inviter ON referral (inviter);
CREATE INDEX referral_code_by_user ON referral_code (username);
