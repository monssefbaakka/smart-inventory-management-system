-- Where a supplier's goods are normally delivered, seeding the delivery warehouse of every order
-- raised against it. Nullable: a supplier trading with sites that are not tracked by location, or
-- one recorded before this column existed, names none.
ALTER TABLE suppliers
    ADD COLUMN default_warehouse_id BIGINT REFERENCES warehouses (id);

CREATE INDEX idx_suppliers_default_warehouse ON suppliers (default_warehouse_id);
