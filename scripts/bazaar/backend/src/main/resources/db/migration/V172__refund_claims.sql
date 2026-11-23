-- A refund's idempotency key names the attempt: it moves on only after the
-- processor definitely refused one, so a cached refusal never blocks the next.
ALTER TABLE payment ADD COLUMN refund_attempt INT NOT NULL DEFAULT 0;

-- An authorization's key likewise names how many times the order's card was
-- declined, so a second card (or the same one, fixed) is really asked again.
CREATE TABLE payment_decline (
    order_id VARCHAR(32) NOT NULL PRIMARY KEY,
    declines INT NOT NULL
);

-- What an order refund in flight asked for, kept beside its claims on the
-- order's cents and lines. The refund sweep finishes the order side from it
-- once the processor confirms the card refund, releases the claims when the
-- processor refused it, and releases a claim no payment claim followed.
-- line_list is "orderLineId:quantity" pairs joined by ';'.
CREATE TABLE order_refund_claim (
    order_id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    amount_cents BIGINT NOT NULL,
    card_cents BIGINT NOT NULL,
    presentment_cents BIGINT NOT NULL,
    gift_cents BIGINT NOT NULL,
    line_list VARCHAR(4000) NOT NULL,
    shipping_cents BIGINT NOT NULL,
    restock INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    claimed_at BIGINT NOT NULL,
    card_done INT NOT NULL
);
