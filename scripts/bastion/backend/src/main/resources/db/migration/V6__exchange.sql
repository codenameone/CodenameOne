-- The crypto exchange: markets, orders, trades, and the addresses coins may
-- be withdrawn to. The balances themselves are ordinary ledger accounts --
-- an order's committed funds sit in the owner's HOLD account -- so nothing
-- here holds money; it records why the ledger moved it.

-- One row per market, written from bastion.markets at start-up. Placing an
-- order writes this row first, which is the lock that makes one market match
-- one order at a time.
CREATE TABLE market (
    id VARCHAR(16) NOT NULL PRIMARY KEY,
    base VARCHAR(12) NOT NULL,
    quote VARCHAR(12) NOT NULL,
    lot BIGINT NOT NULL,
    index_price BIGINT NOT NULL,
    last_price BIGINT NOT NULL,
    seq BIGINT NOT NULL
);

CREATE TABLE trade_order (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    market VARCHAR(16) NOT NULL,
    side VARCHAR(4) NOT NULL,
    type VARCHAR(6) NOT NULL,
    price BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    budget BIGINT NOT NULL,
    filled BIGINT NOT NULL,
    notional BIGINT NOT NULL,
    settled BIGINT NOT NULL,
    fee BIGINT NOT NULL,
    held BIGINT NOT NULL,
    status VARCHAR(10) NOT NULL,
    seq BIGINT NOT NULL,
    client_key VARCHAR(80) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
-- The book: open orders of a market and side, by price then arrival.
CREATE INDEX trade_order_book ON trade_order (market, side, status, price, seq);
CREATE INDEX trade_order_owner ON trade_order (owner, status);
-- What makes a retried order a retry, however many copies arrive at once.
CREATE UNIQUE INDEX trade_order_key ON trade_order (owner, client_key);

CREATE TABLE trade (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    market VARCHAR(16) NOT NULL,
    buy_order VARCHAR(32) NOT NULL,
    sell_order VARCHAR(32) NOT NULL,
    buyer VARCHAR(190) NOT NULL,
    seller VARCHAR(190) NOT NULL,
    taker_side VARCHAR(4) NOT NULL,
    price BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    buyer_paid BIGINT NOT NULL,
    seller_got BIGINT NOT NULL,
    fee BIGINT NOT NULL,
    entry_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX trade_buyer ON trade (buyer, created_at);
CREATE INDEX trade_seller ON trade (seller, created_at);

CREATE TABLE crypto_address (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    asset VARCHAR(12) NOT NULL,
    address VARCHAR(100) NOT NULL,
    label VARCHAR(80) NOT NULL,
    usable_at BIGINT NOT NULL,
    removed SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX crypto_address_owner ON crypto_address (owner);
