CREATE TABLE stock_movements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id),
    type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_stock_movements_product_id ON stock_movements (product_id);
