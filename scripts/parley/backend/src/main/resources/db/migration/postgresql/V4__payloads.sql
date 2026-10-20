-- Envelopes waiting for delivery, one per receiving device. The payload is
-- ciphertext the server cannot read.
CREATE TABLE envelope (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    message_id VARCHAR(32) NOT NULL,
    conversation_id VARCHAR(32) NOT NULL,
    sender VARCHAR(190) NOT NULL,
    sender_device VARCHAR(32) NOT NULL,
    recipient VARCHAR(190) NOT NULL,
    recipient_device VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    header VARCHAR(2048) NOT NULL,
    payload BYTEA NOT NULL,
    sent_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL
);
CREATE INDEX envelope_recipient_device_sent_at ON envelope (recipient_device, sent_at);
CREATE INDEX envelope_expires_at ON envelope (expires_at);
CREATE INDEX envelope_recipient ON envelope (recipient);

-- Encrypted attachments.
CREATE TABLE media_blob (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    size INTEGER NOT NULL,
    created_at BIGINT NOT NULL,
    data BYTEA NOT NULL
);
CREATE INDEX media_blob_created_at ON media_blob (created_at);

-- Profile photos.
CREATE TABLE profile_photo (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    content_type VARCHAR(32) NOT NULL,
    updated_at BIGINT NOT NULL,
    data BYTEA NOT NULL
);

-- Group photos.
CREATE TABLE conversation_photo (
    conversation_id VARCHAR(32) NOT NULL PRIMARY KEY,
    content_type VARCHAR(32) NOT NULL,
    updated_at BIGINT NOT NULL,
    data BYTEA NOT NULL
);
