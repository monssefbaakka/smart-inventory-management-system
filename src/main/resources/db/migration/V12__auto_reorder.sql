-- How many units to order when a product is replenished. Left null the replenishment
-- quantity is derived from the reorder threshold instead.
ALTER TABLE products ADD COLUMN reorder_quantity INTEGER;

ALTER TABLE products ADD CONSTRAINT ck_products_reorder_quantity_positive
    CHECK (reorder_quantity IS NULL OR reorder_quantity > 0);

-- Distinguishes orders raised by the automatic reorder from those entered by hand.
ALTER TABLE purchase_orders ADD COLUMN auto_generated BOOLEAN NOT NULL DEFAULT FALSE;

-- The automatic reorder asks, on every stock movement, whether the product already sits
-- on an open order; that lookup filters on status.
CREATE INDEX idx_purchase_orders_status ON purchase_orders (status);
