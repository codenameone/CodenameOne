-- Everything that matters, in order, each row carrying the SHA-256 of the row
-- before it. Changing, deleting or reordering a row breaks every hash after
-- it, which Audit.verify reports by sequence number. A real bank also ships
-- the newest hash somewhere this database's administrators cannot write (a
-- WORM bucket, a transparency log), because whoever can rewrite this table
-- can rewrite the whole chain.
CREATE TABLE audit_event (
    seq BIGINT NOT NULL PRIMARY KEY,
    at BIGINT NOT NULL,
    actor VARCHAR(190) NOT NULL,
    action VARCHAR(40) NOT NULL,
    subject VARCHAR(190) NOT NULL,
    detail VARCHAR(500) NOT NULL,
    address VARCHAR(64) NOT NULL,
    prev_hash VARCHAR(64) NOT NULL,
    hash VARCHAR(64) NOT NULL
);
CREATE INDEX audit_event_actor ON audit_event (actor, seq);

-- The newest link of the chain, in a row of its own. Appending updates it
-- first, which holds the row until the transaction ends: two appends never
-- read the same tail, on any engine, and an append rolled back with the
-- business change it records leaves no gap.
CREATE TABLE audit_head (
    id INTEGER NOT NULL PRIMARY KEY,
    seq BIGINT NOT NULL,
    hash VARCHAR(64) NOT NULL
);
INSERT INTO audit_head (id, seq, hash) VALUES (1, 0, '0000000000000000000000000000000000000000000000000000000000000000');
