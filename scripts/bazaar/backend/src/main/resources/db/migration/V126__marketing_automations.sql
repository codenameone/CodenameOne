-- The last product a signed-in shopper looked at in a store, for the
-- browse abandonment mail; one row per shopper and store.
CREATE TABLE product_view (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    viewed_at BIGINT NOT NULL,
    mailed_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX product_view_shopper ON product_view (store_id, username);
CREATE INDEX product_view_viewed ON product_view (viewed_at);
-- A review request mailed some days after an order was delivered.
CREATE TABLE review_request (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    email VARCHAR(190) NOT NULL,
    due_at BIGINT NOT NULL,
    sent_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX review_request_order ON review_request (order_id);
CREATE INDEX review_request_due ON review_request (sent_at, due_at);
-- A gift card mailed to someone, with a message, on a chosen day.
ALTER TABLE gift_card ADD COLUMN recipient_email VARCHAR(190) NOT NULL DEFAULT '';
ALTER TABLE gift_card ADD COLUMN recipient_name VARCHAR(80) NOT NULL DEFAULT '';
ALTER TABLE gift_card ADD COLUMN message VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE gift_card ADD COLUMN deliver_at BIGINT NOT NULL DEFAULT 0;
ALTER TABLE gift_card ADD COLUMN delivered_at BIGINT NOT NULL DEFAULT 0;
