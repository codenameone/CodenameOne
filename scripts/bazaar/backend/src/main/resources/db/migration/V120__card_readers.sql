-- Card readers paired with a store and placed at a register, and the card
-- payments pushed to them (server-driven, the way Stripe Terminal works).
CREATE TABLE card_reader (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    register_id VARCHAR(32) NOT NULL,
    provider VARCHAR(16) NOT NULL,
    provider_ref VARCHAR(64) NOT NULL,
    label VARCHAR(80) NOT NULL,
    serial VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX card_reader_store ON card_reader (store_id);
CREATE TABLE card_payment (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    register_id VARCHAR(32) NOT NULL,
    reader_id VARCHAR(32) NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    intent_ref VARCHAR(64) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    message VARCHAR(200) NOT NULL,
    card_label VARCHAR(40) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX card_payment_client ON card_payment (store_id, client_id);
CREATE INDEX card_payment_intent ON card_payment (intent_ref);
