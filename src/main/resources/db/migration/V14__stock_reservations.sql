CREATE TABLE stock_reservations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants (slug),
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    warehouse_id BIGINT REFERENCES warehouses (id),
    reference VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_stock_reservations_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_stock_reservations_tenant ON stock_reservations (tenant_id);
CREATE INDEX idx_stock_reservations_warehouse ON stock_reservations (warehouse_id);

-- Every availability answer sums the held reservations of one product, and the listings read one
-- product's reservations, so the product and its status are always looked at together.
CREATE INDEX idx_stock_reservations_product_status ON stock_reservations (product_id, status);
