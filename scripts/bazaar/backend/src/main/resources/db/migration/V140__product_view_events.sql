-- Every product page a visitor opened: the funnel's first step and the
-- purchase-intent signals of the sales pipeline (views, repeat visits, time
-- on product). A visitor is the cart token of the browser, else the account.
CREATE TABLE product_view_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    visitor VARCHAR(190) NOT NULL,
    viewed_at BIGINT NOT NULL
);
CREATE INDEX product_view_event_store ON product_view_event (store_id, viewed_at);
CREATE INDEX product_view_event_visitor ON product_view_event (store_id, visitor);
