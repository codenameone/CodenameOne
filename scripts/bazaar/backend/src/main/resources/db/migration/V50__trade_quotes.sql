-- Wave B: B2B quotes and net-terms orders.
CREATE TABLE trade_quote (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    company_id VARCHAR(32) NOT NULL,
    buyer_username VARCHAR(190) NOT NULL,
    status VARCHAR(16) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    subtotal_cents BIGINT NOT NULL,
    discount_cents BIGINT NOT NULL,
    total_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE TABLE trade_quote_line (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    quote_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL,
    list_cents BIGINT NOT NULL,
    unit_cents BIGINT NOT NULL
);
CREATE TABLE net_term (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    company_id VARCHAR(32) NOT NULL,
    terms_days INTEGER NOT NULL,
    due_at BIGINT NOT NULL,
    paid_at BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL
);
CREATE INDEX trade_quote_store_id_status ON trade_quote (store_id, status);
CREATE INDEX trade_quote_company_id ON trade_quote (company_id);
CREATE INDEX trade_quote_line_quote_id ON trade_quote_line (quote_id);
CREATE INDEX trade_quote_line_store_id ON trade_quote_line (store_id);
CREATE INDEX net_term_store_id_status ON net_term (store_id, status);
