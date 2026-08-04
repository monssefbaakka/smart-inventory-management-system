-- What a product's units on hand cost, as a weighted average. It is never written directly: every
-- value it takes is the consequence of a receipt, so existing rows start at zero and are corrected
-- by the first receipt that carries a cost.
ALTER TABLE products
    ADD COLUMN average_cost NUMERIC(12, 4) NOT NULL DEFAULT 0;

ALTER TABLE products
    ADD CONSTRAINT ck_products_average_cost_not_negative CHECK (average_cost >= 0);

-- What each movement was valued at when it happened. Left null on the movements recorded before
-- costing existed: their cost is not zero, it is unknown, and a zero would understate every total
-- computed from them.
ALTER TABLE stock_movements
    ADD COLUMN unit_cost NUMERIC(12, 4);

ALTER TABLE stock_movements
    ADD COLUMN total_cost NUMERIC(14, 4);

-- The cost-of-goods-sold report reads the OUT movements of a window, either across the catalogue or
-- for one product, so the type and the moment are always looked at together.
CREATE INDEX idx_stock_movements_type_created ON stock_movements (type, created_at);
CREATE INDEX idx_stock_movements_product_type_created ON stock_movements (product_id, type, created_at);
