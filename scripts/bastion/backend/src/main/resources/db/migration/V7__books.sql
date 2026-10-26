-- The accounting layer over the ledger: reversals, closed periods and
-- four-eyes approvals. Portable across SQLite, PostgreSQL and MySQL like the
-- scripts before it. The chart of accounts is code (books.Chart), not a
-- table: an account's class follows from its kind and cannot drift.

-- One row per reversed entry, keyed by the entry, so an entry is reversed at
-- most once even when two administrators try together: the second insert
-- fails on the key and its reversing entry rolls back with it.
CREATE TABLE reversal (
    entry_id VARCHAR(32) NOT NULL PRIMARY KEY,
    reversed_by VARCHAR(32) NOT NULL,
    at BIGINT NOT NULL
);

-- Each closed period, in order. Nothing may be posted at or before the
-- newest closed_through; the closing entries that moved the period's revenue
-- and expense into retained earnings are listed in `entries`.
CREATE TABLE period_close (
    seq BIGINT NOT NULL PRIMARY KEY,
    closed_through BIGINT NOT NULL,
    closed_by VARCHAR(190) NOT NULL,
    closed_at BIGINT NOT NULL,
    entries VARCHAR(500) NOT NULL
);

-- An administrator's adjustment or reversal. Above the threshold it waits,
-- PENDING, for a different administrator to approve it (maker-checker);
-- what it will post is fixed when it is made, so the checker approves exactly
-- what they saw.
CREATE TABLE approval (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    kind VARCHAR(16) NOT NULL,
    status VARCHAR(10) NOT NULL,
    maker VARCHAR(190) NOT NULL,
    checker VARCHAR(190) NOT NULL,
    account_id VARCHAR(32) NOT NULL,
    entry_id VARCHAR(32) NOT NULL,
    asset VARCHAR(12) NOT NULL,
    amount BIGINT NOT NULL,
    usd_cents BIGINT NOT NULL,
    memo VARCHAR(140) NOT NULL,
    request_key VARCHAR(80) NOT NULL,
    result_entry VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    decided_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX approval_key ON approval (request_key);
CREATE INDEX approval_status ON approval (status, created_at);

-- The reports read by time.
CREATE INDEX posting_time ON posting (created_at);
CREATE INDEX journal_entry_time ON journal_entry (created_at);
CREATE INDEX trade_market_time ON trade (market, created_at);
CREATE INDEX audit_event_subject ON audit_event (subject, seq);
CREATE INDEX audit_event_action ON audit_event (action, seq);
