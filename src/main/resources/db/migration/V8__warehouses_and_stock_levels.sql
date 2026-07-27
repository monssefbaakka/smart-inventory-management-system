CREATE TABLE warehouses (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE stock_levels (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_stock_levels_product_warehouse UNIQUE (product_id, warehouse_id)
);

CREATE INDEX idx_stock_levels_warehouse ON stock_levels (warehouse_id);

ALTER TABLE stock_movements
    ADD COLUMN warehouse_id BIGINT REFERENCES warehouses (id);

CREATE INDEX idx_stock_movements_warehouse ON stock_movements (warehouse_id);
