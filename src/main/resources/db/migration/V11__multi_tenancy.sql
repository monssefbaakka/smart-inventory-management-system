CREATE TABLE tenants (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO tenants (slug, name, active, created_at, updated_at)
VALUES ('default', 'Default', TRUE, NOW(), NOW());

-- Every tenant-owned table carries the tenant slug as a discriminator. Existing rows
-- predate multi-tenancy and therefore belong to the 'default' tenant.
ALTER TABLE users ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE categories ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE suppliers ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE products ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE warehouses ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE stock_levels ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE stock_movements ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE stock_transfers ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE stock_counts ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE purchase_orders ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE audit_logs ADD COLUMN tenant_id VARCHAR(64);

UPDATE users SET tenant_id = 'default';
UPDATE categories SET tenant_id = 'default';
UPDATE suppliers SET tenant_id = 'default';
UPDATE products SET tenant_id = 'default';
UPDATE warehouses SET tenant_id = 'default';
UPDATE stock_levels SET tenant_id = 'default';
UPDATE stock_movements SET tenant_id = 'default';
UPDATE stock_transfers SET tenant_id = 'default';
UPDATE stock_counts SET tenant_id = 'default';
UPDATE purchase_orders SET tenant_id = 'default';
UPDATE audit_logs SET tenant_id = 'default';

ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE categories ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE suppliers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE products ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE warehouses ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE stock_levels ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE stock_movements ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE stock_transfers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE stock_counts ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE purchase_orders ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (slug);
ALTER TABLE categories ADD CONSTRAINT fk_categories_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (slug);
ALTER TABLE suppliers ADD CONSTRAINT fk_suppliers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (slug);
ALTER TABLE products ADD CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (slug);
ALTER TABLE warehouses ADD CONSTRAINT fk_warehouses_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (slug);
ALTER TABLE stock_levels ADD CONSTRAINT fk_stock_levels_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (slug);
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (slug);
ALTER TABLE stock_transfers ADD CONSTRAINT fk_stock_transfers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (slug);
ALTER TABLE stock_counts ADD CONSTRAINT fk_stock_counts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (slug);
ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (slug);
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (slug);

-- Business keys are unique within a tenant, not across the whole installation.
-- Login stays global: a user's email identifies the account and the tenant it belongs to.
ALTER TABLE categories DROP CONSTRAINT categories_name_key;
ALTER TABLE categories ADD CONSTRAINT uk_categories_tenant_name UNIQUE (tenant_id, name);

ALTER TABLE suppliers DROP CONSTRAINT suppliers_email_key;
ALTER TABLE suppliers ADD CONSTRAINT uk_suppliers_tenant_email UNIQUE (tenant_id, email);

ALTER TABLE products DROP CONSTRAINT products_sku_key;
ALTER TABLE products ADD CONSTRAINT uk_products_tenant_sku UNIQUE (tenant_id, sku);

ALTER TABLE products DROP CONSTRAINT uk_products_barcode;
ALTER TABLE products ADD CONSTRAINT uk_products_tenant_barcode UNIQUE (tenant_id, barcode);

ALTER TABLE warehouses DROP CONSTRAINT warehouses_code_key;
ALTER TABLE warehouses ADD CONSTRAINT uk_warehouses_tenant_code UNIQUE (tenant_id, code);

CREATE INDEX idx_users_tenant ON users (tenant_id);
CREATE INDEX idx_categories_tenant ON categories (tenant_id);
CREATE INDEX idx_suppliers_tenant ON suppliers (tenant_id);
CREATE INDEX idx_products_tenant ON products (tenant_id);
CREATE INDEX idx_warehouses_tenant ON warehouses (tenant_id);
CREATE INDEX idx_stock_levels_tenant ON stock_levels (tenant_id);
CREATE INDEX idx_stock_movements_tenant ON stock_movements (tenant_id);
CREATE INDEX idx_stock_transfers_tenant ON stock_transfers (tenant_id);
CREATE INDEX idx_stock_counts_tenant ON stock_counts (tenant_id);
CREATE INDEX idx_purchase_orders_tenant ON purchase_orders (tenant_id);
CREATE INDEX idx_audit_logs_tenant ON audit_logs (tenant_id);
