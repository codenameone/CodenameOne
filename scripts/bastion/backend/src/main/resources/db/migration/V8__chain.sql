-- Coins crossing to and from a real blockchain (chain/). Empty and unused
-- unless bastion.chain.rpc-url names a node.

CREATE TABLE chain_transfer (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    direction VARCHAR(3) NOT NULL,
    owner VARCHAR(190) NOT NULL,
    asset VARCHAR(12) NOT NULL,
    address VARCHAR(42) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(12) NOT NULL,
    tx_hash VARCHAR(66) NOT NULL,
    log_index INTEGER NOT NULL,
    nonce BIGINT NOT NULL,
    raw VARCHAR(4000) NOT NULL,
    block_number BIGINT NOT NULL,
    block_hash VARCHAR(66) NOT NULL,
    fee BIGINT NOT NULL,
    reference VARCHAR(32) NOT NULL,
    detail VARCHAR(200) NOT NULL,
    broadcast_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX chain_transfer_owner ON chain_transfer (owner);
CREATE INDEX chain_transfer_status ON chain_transfer (status);
-- A deposit is credited once however often its block is scanned. A
-- withdrawal not yet signed holds "unsigned:" and its own id in tx_hash, so
-- it never collides.
CREATE UNIQUE INDEX chain_transfer_tx ON chain_transfer (direction, tx_hash, log_index);

CREATE TABLE deposit_address (
    address VARCHAR(42) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    asset VARCHAR(12) NOT NULL,
    hd_index BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX deposit_address_index ON deposit_address (hd_index);
CREATE UNIQUE INDEX deposit_address_owner ON deposit_address (owner, asset);

CREATE TABLE chain_cursor (
    chain VARCHAR(12) NOT NULL PRIMARY KEY,
    next_block BIGINT NOT NULL,
    last_hash VARCHAR(66) NOT NULL,
    updated_at BIGINT NOT NULL
);
