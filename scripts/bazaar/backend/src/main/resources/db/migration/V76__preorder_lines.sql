-- Pre-orders at checkout: a line sold ahead of stock, holding no stock.
ALTER TABLE order_line ADD COLUMN preorder SMALLINT NOT NULL DEFAULT 0;
