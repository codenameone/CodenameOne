-- Wave B: payment disputes (chargebacks) and the evidence answering them.
CREATE TABLE dispute (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    reason VARCHAR(40) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    due_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX dispute_event_key ON dispute (event_id);
CREATE INDEX dispute_store_id ON dispute (store_id);
CREATE TABLE dispute_evidence (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    dispute_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    body TEXT NOT NULL,
    created_by VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX dispute_evidence_dispute ON dispute_evidence (dispute_id);
