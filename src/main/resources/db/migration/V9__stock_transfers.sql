CREATE TABLE stock_transfers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    source_warehouse_id BIGINT NOT NULL REFERENCES warehouses (id),
    destination_warehouse_id BIGINT NOT NULL REFERENCES warehouses (id),
    quantity INTEGER NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_stock_transfers_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_stock_transfers_distinct_warehouses CHECK (source_warehouse_id <> destination_warehouse_id)
);

CREATE INDEX idx_stock_transfers_product ON stock_transfers (product_id);
CREATE INDEX idx_stock_transfers_source ON stock_transfers (source_warehouse_id);
CREATE INDEX idx_stock_transfers_destination ON stock_transfers (destination_warehouse_id);
