-- What the app knows about an account beyond its sign-in.
CREATE TABLE profile (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    phone_verified SMALLINT NOT NULL,
    about VARCHAR(255) NOT NULL,
    has_photo SMALLINT NOT NULL,
    created_at BIGINT NOT NULL,
    last_seen_at BIGINT NOT NULL,
    language VARCHAR(8) NOT NULL,
    flagged SMALLINT NOT NULL,
    flag_reason VARCHAR(255) NOT NULL
);

-- How one person wants the app and what they let others see.
CREATE TABLE preference (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    theme VARCHAR(8) NOT NULL,
    notify_messages SMALLINT NOT NULL,
    notify_groups SMALLINT NOT NULL,
    notify_calls SMALLINT NOT NULL,
    read_receipts SMALLINT NOT NULL,
    typing_indicators SMALLINT NOT NULL,
    last_seen VARCHAR(16) NOT NULL,
    profile_photo VARCHAR(16) NOT NULL,
    disappearing_seconds INTEGER NOT NULL,
    screen_security SMALLINT NOT NULL,
    app_lock SMALLINT NOT NULL
);

-- The devices of each account and the public half of their keys.
CREATE TABLE device (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    name VARCHAR(120) NOT NULL,
    platform VARCHAR(16) NOT NULL,
    identity_key VARCHAR(200) NOT NULL,
    signed_pre_key_id INTEGER NOT NULL,
    signed_pre_key VARCHAR(200) NOT NULL,
    signed_pre_key_signature VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL,
    last_seen_at BIGINT NOT NULL
);
CREATE INDEX device_username ON device (username);

-- One-time prekeys; each row is deleted as it is handed out.
CREATE TABLE one_time_pre_key (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    device_id VARCHAR(32) NOT NULL,
    key_id INTEGER NOT NULL,
    public_key VARCHAR(200) NOT NULL
);
CREATE INDEX one_time_pre_key_device_id ON one_time_pre_key (device_id);

-- Conversations. direct_key is the pair of usernames of a direct chat, in
-- order, and unique, so the same two people never get two even when both
-- start the chat at once; a group's is '#' and its id.
CREATE TABLE conversation (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    kind VARCHAR(16) NOT NULL,
    title VARCHAR(120) NOT NULL,
    about VARCHAR(255) NOT NULL,
    disappearing_seconds INTEGER NOT NULL,
    admins_only SMALLINT NOT NULL,
    has_photo SMALLINT NOT NULL,
    created_by VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL,
    last_activity_at BIGINT NOT NULL,
    direct_key VARCHAR(400) NOT NULL
);
CREATE UNIQUE INDEX conversation_direct_key ON conversation (direct_key);

-- Who is in which conversation, in which role, and how they see it.
CREATE TABLE membership (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    conversation_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    role VARCHAR(8) NOT NULL,
    muted SMALLINT NOT NULL,
    archived SMALLINT NOT NULL,
    pinned SMALLINT NOT NULL,
    joined_at BIGINT NOT NULL
);
CREATE INDEX membership_conversation_id ON membership (conversation_id);
CREATE INDEX membership_username ON membership (username);

-- Invite links. Revoking one deletes its row.
CREATE TABLE invite_link (
    code VARCHAR(32) NOT NULL PRIMARY KEY,
    conversation_id VARCHAR(32) NOT NULL,
    created_by VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL
);
CREATE INDEX invite_link_conversation_id ON invite_link (conversation_id);

-- One row per message sent. Never holds content; the id is a hash of the
-- sender and the sender's own id for the message, so a retry is recognised.
CREATE TABLE message_log (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    message_id VARCHAR(32) NOT NULL,
    conversation_id VARCHAR(32) NOT NULL,
    sender VARCHAR(190) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    sent_at BIGINT NOT NULL
);
CREATE INDEX message_log_sent_at ON message_log (sent_at);
CREATE INDEX message_log_sender ON message_log (sender);

-- Blocks. The id is the blocker and the blocked account, hashed.
CREATE TABLE contact_block (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    blocker VARCHAR(190) NOT NULL,
    blocked VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX contact_block_blocker ON contact_block (blocker);
CREATE INDEX contact_block_blocked ON contact_block (blocked);

-- Abuse reports. evidence is what the reporter chose to decrypt and share.
CREATE TABLE report (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    reporter VARCHAR(190) NOT NULL,
    subject VARCHAR(190) NOT NULL,
    conversation_id VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    evidence VARCHAR(4000) NOT NULL,
    created_at BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    resolved_by VARCHAR(190) NOT NULL,
    outcome VARCHAR(255) NOT NULL
);
CREATE INDEX report_status ON report (status);
CREATE INDEX report_subject ON report (subject);

-- Calls, for the call history and the ringing state machine.
CREATE TABLE call_record (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    conversation_id VARCHAR(32) NOT NULL,
    caller VARCHAR(190) NOT NULL,
    caller_device VARCHAR(32) NOT NULL,
    callee VARCHAR(190) NOT NULL,
    callee_device VARCHAR(32) NOT NULL,
    video SMALLINT NOT NULL,
    state VARCHAR(16) NOT NULL,
    started_at BIGINT NOT NULL,
    answered_at BIGINT NOT NULL,
    ended_at BIGINT NOT NULL
);
CREATE INDEX call_record_caller ON call_record (caller);
CREATE INDEX call_record_callee ON call_record (callee);

-- The call each person is on, keyed by username, so that the database refuses
-- a second live call for anyone however two callers' transactions interleave.
CREATE TABLE call_line (
    id VARCHAR(190) NOT NULL PRIMARY KEY,
    call_id VARCHAR(32) NOT NULL
);
CREATE INDEX call_line_call_id ON call_line (call_id);

-- Bridges. A bridged conversation is not end-to-end encrypted.
CREATE TABLE bridge_link (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    conversation_id VARCHAR(32) NOT NULL,
    network VARCHAR(16) NOT NULL,
    external_chat VARCHAR(190) NOT NULL,
    label VARCHAR(120) NOT NULL,
    created_by VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
-- One link per conversation and one per external chat. The checks in
-- Bridges read first; these decide when two admins link at once.
CREATE UNIQUE INDEX bridge_link_conversation_id ON bridge_link (conversation_id);
CREATE UNIQUE INDEX bridge_link_external_chat ON bridge_link (network, external_chat);

-- Webhook events already posted, by the network's own event or message id:
-- a provider retries a delivery whose answer it did not see.
CREATE TABLE bridge_event (
    id VARCHAR(240) NOT NULL PRIMARY KEY,
    received_at BIGINT NOT NULL
);
CREATE INDEX bridge_event_received_at ON bridge_event (received_at);
