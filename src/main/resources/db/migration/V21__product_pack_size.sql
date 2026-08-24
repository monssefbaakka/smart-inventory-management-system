-- How many units the supplier ships together, when the product is sold in packs. Nullable: a
-- product recorded before this column existed, or one bought a unit at a time, names none and is
-- ordered in whatever quantity the reorder rule arrives at.
ALTER TABLE products ADD COLUMN pack_size INTEGER;

ALTER TABLE products ADD CONSTRAINT ck_products_pack_size_positive
    CHECK (pack_size IS NULL OR pack_size > 0);
