-- Archiving is a flag beside the order's status: an archived order keeps
-- its state and leaves the working list. The store's order settings hold
-- auto-archiving and the return rules.
ALTER TABLE store_order ADD COLUMN archived SMALLINT NOT NULL DEFAULT 0;
CREATE TABLE order_settings (
    store_id VARCHAR(32) NOT NULL PRIMARY KEY,
    auto_archive SMALLINT NOT NULL,
    return_window_days INTEGER NOT NULL,
    restocking_fee_bp INTEGER NOT NULL,
    return_shipping_cents BIGINT NOT NULL,
    final_sale_products VARCHAR(2000) NOT NULL,
    final_sale_collections VARCHAR(2000) NOT NULL,
    updated_at BIGINT NOT NULL
);
