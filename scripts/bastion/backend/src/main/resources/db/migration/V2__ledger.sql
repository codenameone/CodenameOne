-- The ledger. Every script here is written once for SQLite, PostgreSQL and
-- MySQL: no generated keys, no native boolean or timestamp. A yes/no is a
-- SMALLINT 0/1 and a time is milliseconds since the epoch in a BIGINT.
--
-- Money is a BIGINT count of an asset's smallest ledger unit, never a float:
-- cents for USD, satoshi for BTC. See asset.decimals.

-- What can be held. Fiat and crypto are the same thing to the ledger: a code
-- and how many decimal places one unit of it is divided into.
CREATE TABLE asset (
    code VARCHAR(12) NOT NULL PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    kind VARCHAR(8) NOT NULL,
    decimals INTEGER NOT NULL,
    enabled SMALLINT NOT NULL
);

-- A balance in one asset. Customers' accounts may never go below zero; the
-- bank's own (kind starting with SYS_) are the other side of every deposit and
-- withdrawal and may. `balance` is kept by the same transaction that writes
-- the postings, by a conditional update, and can always be recomputed from
-- them -- Ledger.verify does exactly that.
CREATE TABLE ledger_account (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    asset VARCHAR(12) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    name VARCHAR(80) NOT NULL,
    balance BIGINT NOT NULL,
    frozen SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX ledger_account_owner ON ledger_account (owner, asset);

-- One movement of value: a set of postings that sums to zero in every asset.
-- `idempotency_key` is unique, so the same request retried after a dropped
-- connection is answered with the entry it already made and moves nothing.
-- `request_hash` is what that request said, so a different request reusing
-- the key is refused rather than answered with somebody else's result.
CREATE TABLE journal_entry (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    idempotency_key VARCHAR(80) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    initiator VARCHAR(190) NOT NULL,
    memo VARCHAR(140) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX journal_entry_key ON journal_entry (idempotency_key);

-- One leg of an entry. Positive adds to the account, negative takes from it.
-- Rows are only ever inserted: a mistake is corrected by a reversing entry.
CREATE TABLE posting (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    entry_id VARCHAR(32) NOT NULL,
    account_id VARCHAR(32) NOT NULL,
    asset VARCHAR(12) NOT NULL,
    amount BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX posting_account ON posting (account_id, created_at);
CREATE INDEX posting_entry ON posting (entry_id);

-- Assets 1 unit = 10^decimals ledger units. ETH has 18 decimals on its chain;
-- the ledger keeps 9 (gwei) so that a balance fits a BIGINT with room to
-- spare -- 9.2e18 wei is only 9.2 ETH. A bridge to a real chain rounds at the
-- edge, which is where a real exchange rounds too.
INSERT INTO asset (code, name, kind, decimals, enabled) VALUES ('USD', 'US Dollar', 'FIAT', 2, 1);
INSERT INTO asset (code, name, kind, decimals, enabled) VALUES ('EUR', 'Euro', 'FIAT', 2, 1);
INSERT INTO asset (code, name, kind, decimals, enabled) VALUES ('BTC', 'Bitcoin', 'CRYPTO', 8, 1);
INSERT INTO asset (code, name, kind, decimals, enabled) VALUES ('ETH', 'Ether', 'CRYPTO', 9, 1);
