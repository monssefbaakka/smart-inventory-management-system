CREATE TABLE stock_counts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses (id),
    status VARCHAR(20) NOT NULL,
    note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE TABLE stock_count_lines (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    stock_count_id BIGINT NOT NULL REFERENCES stock_counts (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id),
    counted_quantity INTEGER NOT NULL,
    expected_quantity INTEGER NOT NULL,
    CONSTRAINT uk_stock_count_lines_count_product UNIQUE (stock_count_id, product_id),
    CONSTRAINT ck_stock_count_lines_counted_not_negative CHECK (counted_quantity >= 0),
    CONSTRAINT ck_stock_count_lines_expected_not_negative CHECK (expected_quantity >= 0)
);

CREATE INDEX idx_stock_counts_warehouse ON stock_counts (warehouse_id);
CREATE INDEX idx_stock_counts_status ON stock_counts (status);
CREATE INDEX idx_stock_count_lines_product ON stock_count_lines (product_id);
