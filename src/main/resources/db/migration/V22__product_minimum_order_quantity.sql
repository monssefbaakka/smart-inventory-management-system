-- The fewest units the supplier will accept on an order, when they impose one. Nullable: a product
-- recorded before this column existed, or one a supplier will sell in any quantity, names none and
-- is ordered in whatever quantity the reorder rule arrives at.
ALTER TABLE products ADD COLUMN minimum_order_quantity INTEGER;

ALTER TABLE products ADD CONSTRAINT ck_products_minimum_order_quantity_positive
    CHECK (minimum_order_quantity IS NULL OR minimum_order_quantity > 0);
