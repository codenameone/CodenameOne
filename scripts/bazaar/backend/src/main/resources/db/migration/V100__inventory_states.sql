-- Selling past zero marks the units an order line could not be given from
-- stock; and the units of a level that are on the premises but cannot be
-- sold, in three buckets of their own. Bucketed units are out of on_hand, so
-- every "available = on_hand - committed" stays true without a change.
ALTER TABLE order_line ADD COLUMN backordered_qty INTEGER NOT NULL DEFAULT 0;
ALTER TABLE inventory_level ADD COLUMN damaged INTEGER NOT NULL DEFAULT 0;
ALTER TABLE inventory_level ADD COLUMN quality_control INTEGER NOT NULL DEFAULT 0;
ALTER TABLE inventory_level ADD COLUMN safety_stock INTEGER NOT NULL DEFAULT 0;
