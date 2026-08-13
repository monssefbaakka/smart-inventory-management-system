-- How low one warehouse is allowed to get on one product before it orders for itself. Nullable: a
-- level recorded before this column existed, or one belonging to a site that is content to be
-- measured as part of the product total, names none and is not measured on its own.
ALTER TABLE stock_levels
    ADD COLUMN reorder_threshold INTEGER;
