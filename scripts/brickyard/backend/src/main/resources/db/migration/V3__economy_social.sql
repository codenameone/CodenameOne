-- Economy, progression, social, moderation and analytics. Portable SQL only,
-- as in V1: string keys, SMALLINT-free integers, epoch-millisecond times.

CREATE TABLE ledger_entry (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    amount BIGINT NOT NULL,
    kind VARCHAR(32) NOT NULL,
    reference VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX ledger_entry_username ON ledger_entry (username);
CREATE INDEX ledger_entry_created_at ON ledger_entry (created_at);

CREATE TABLE store_purchase (
    id VARCHAR(400) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    bricks BIGINT NOT NULL,
    refunded INTEGER NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX store_purchase_created_at ON store_purchase (created_at);

CREATE TABLE product (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    name VARCHAR(80) NOT NULL,
    price BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX product_experience_id ON product (experience_id);

CREATE TABLE ownership (
    id VARCHAR(400) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    experience_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    price BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX ownership_username ON ownership (username);
CREATE INDEX ownership_product_id ON ownership (product_id);
CREATE INDEX ownership_experience_id ON ownership (experience_id);

CREATE TABLE catalog_item (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    slot VARCHAR(32) NOT NULL,
    price BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE plus_subscription (
    id VARCHAR(190) NOT NULL PRIMARY KEY,
    expires_at BIGINT NOT NULL,
    last_stipend_at BIGINT NOT NULL
);

CREATE TABLE play_time (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    seconds BIGINT NOT NULL,
    reported_at BIGINT NOT NULL,
    paid_out INTEGER NOT NULL
);
CREATE INDEX play_time_paid_out ON play_time (paid_out);

CREATE TABLE payout_request (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    bricks BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    decided_by VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX payout_request_status ON payout_request (status);

CREATE TABLE progress (
    id VARCHAR(190) NOT NULL PRIMARY KEY,
    xp BIGINT NOT NULL,
    season_xp BIGINT NOT NULL,
    premium INTEGER NOT NULL,
    streak INTEGER NOT NULL,
    last_login_day BIGINT NOT NULL
);

CREATE TABLE quest (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    title VARCHAR(80) NOT NULL,
    cadence VARCHAR(16) NOT NULL,
    event_name VARCHAR(64) NOT NULL,
    target BIGINT NOT NULL,
    reward_xp BIGINT NOT NULL,
    reward_bricks BIGINT NOT NULL,
    active INTEGER NOT NULL
);
CREATE INDEX quest_active ON quest (active);

CREATE TABLE quest_progress (
    id VARCHAR(400) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    quest_id VARCHAR(32) NOT NULL,
    period_start BIGINT NOT NULL,
    progress BIGINT NOT NULL,
    claimed INTEGER NOT NULL
);

CREATE TABLE season_claim (
    id VARCHAR(400) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    tier INTEGER NOT NULL,
    track VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE friend_request (
    id VARCHAR(400) NOT NULL PRIMARY KEY,
    from_user VARCHAR(190) NOT NULL,
    to_user VARCHAR(190) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX friend_request_to_user ON friend_request (to_user);
CREATE INDEX friend_request_status ON friend_request (status);
CREATE INDEX friend_request_from_user ON friend_request (from_user);
CREATE INDEX friend_request_created_at ON friend_request (created_at);

CREATE TABLE follow (
    id VARCHAR(400) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    creator VARCHAR(190) NOT NULL
);
CREATE INDEX follow_username ON follow (username);
CREATE INDEX follow_creator ON follow (creator);

CREATE TABLE presence (
    id VARCHAR(190) NOT NULL PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    experience_id VARCHAR(32) NOT NULL,
    server_id VARCHAR(64) NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX presence_updated_at ON presence (updated_at);

CREATE TABLE user_block (
    id VARCHAR(400) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    blocked VARCHAR(190) NOT NULL
);
CREATE INDEX user_block_username ON user_block (username);

CREATE TABLE party (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    leader VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE party_member (
    id VARCHAR(400) NOT NULL PRIMARY KEY,
    party_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    joined_at BIGINT NOT NULL
);
CREATE INDEX party_member_party_id ON party_member (party_id);
CREATE INDEX party_member_username ON party_member (username);

CREATE TABLE chat_message (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    channel VARCHAR(400) NOT NULL,
    sender VARCHAR(190) NOT NULL,
    body VARCHAR(400) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX chat_message_channel ON chat_message (channel);
CREATE INDEX chat_message_created_at ON chat_message (created_at);
CREATE INDEX chat_message_sender ON chat_message (sender);

CREATE TABLE invite (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    inviter VARCHAR(190) NOT NULL,
    device_hash VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE referral (
    id VARCHAR(190) NOT NULL PRIMARY KEY,
    inviter VARCHAR(190) NOT NULL,
    code VARCHAR(32) NOT NULL,
    device_hash VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX referral_inviter ON referral (inviter);
CREATE INDEX referral_created_at ON referral (created_at);

CREATE TABLE fraud_flag (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    detail VARCHAR(400) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX fraud_flag_created_at ON fraud_flag (created_at);

CREATE TABLE report (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    reporter VARCHAR(190) NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id VARCHAR(190) NOT NULL,
    reason VARCHAR(400) NOT NULL,
    status VARCHAR(16) NOT NULL,
    resolved_by VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX report_status ON report (status);

CREATE TABLE sanction (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    reason VARCHAR(400) NOT NULL,
    until_at BIGINT NOT NULL,
    lifted INTEGER NOT NULL,
    issued_by VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX sanction_username ON sanction (username);

CREATE TABLE audit_log (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    actor VARCHAR(190) NOT NULL,
    action VARCHAR(32) NOT NULL,
    target VARCHAR(190) NOT NULL,
    detail VARCHAR(400) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX audit_log_created_at ON audit_log (created_at);

CREATE TABLE appeal (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    sanction_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    decided_by VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX appeal_status ON appeal (status);

CREATE TABLE featured (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    row_name VARCHAR(64) NOT NULL,
    experience_id VARCHAR(32) NOT NULL,
    ordinal INTEGER NOT NULL
);

CREATE TABLE analytics_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    detail VARCHAR(4000) NOT NULL,
    version INTEGER NOT NULL,
    day_start BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX analytics_event_experience_id ON analytics_event (experience_id);
CREATE INDEX analytics_event_day_start ON analytics_event (day_start);
CREATE INDEX analytics_event_kind ON analytics_event (kind);
