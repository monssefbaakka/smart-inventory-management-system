-- Where an order is to be delivered. Nullable: an order raised before this column existed named no
-- location, and one raised for stock that is not tracked by location still names none.
ALTER TABLE purchase_orders
    ADD COLUMN warehouse_id BIGINT REFERENCES warehouses (id);

CREATE INDEX idx_purchase_orders_warehouse ON purchase_orders (warehouse_id);
