-- Shoppers' requests about their personal data at a store: an export of it
-- or its erasure. The store completes them from the back office.
CREATE TABLE data_request (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    done_at BIGINT NOT NULL,
    done_by VARCHAR(190) NOT NULL
);
CREATE INDEX data_request_store ON data_request (store_id, status);
CREATE INDEX data_request_username ON data_request (username);
