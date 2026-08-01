CREATE TABLE product_batches (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants (slug),
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    warehouse_id BIGINT REFERENCES warehouses (id),
    lot_code VARCHAR(64) NOT NULL,
    expiry_date DATE,
    quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_product_batches_tenant_product_lot UNIQUE (tenant_id, product_id, lot_code),
    CONSTRAINT ck_product_batches_quantity_not_negative CHECK (quantity >= 0)
);

CREATE INDEX idx_product_batches_tenant ON product_batches (tenant_id);
CREATE INDEX idx_product_batches_product ON product_batches (product_id);
CREATE INDEX idx_product_batches_warehouse ON product_batches (warehouse_id);

-- Both the earliest-expiry-first allocation and the expiry reports order by expiry date over the
-- lots that still hold stock.
CREATE INDEX idx_product_batches_expiry ON product_batches (expiry_date);

-- A movement may name the lot it applied to, so the history explains which stock moved.
ALTER TABLE stock_movements ADD COLUMN batch_id BIGINT REFERENCES product_batches (id);

CREATE INDEX idx_stock_movements_batch ON stock_movements (batch_id);
