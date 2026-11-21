-- Wave C: the storefront chat widget's conversations and the inbox's canned replies.
CREATE TABLE chat_conversation (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    token VARCHAR(64) NOT NULL,
    email VARCHAR(200) NOT NULL,
    name VARCHAR(100) NOT NULL,
    subject VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL,
    assignee VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX chat_conversation_token ON chat_conversation (token);
CREATE INDEX chat_conversation_store ON chat_conversation (store_id, updated_at);
CREATE TABLE chat_message (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    conversation_id VARCHAR(32) NOT NULL,
    author VARCHAR(200) NOT NULL,
    staff INTEGER NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX chat_message_conversation ON chat_message (conversation_id, created_at);
CREATE TABLE canned_reply (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    title VARCHAR(80) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX canned_reply_store ON canned_reply (store_id);
