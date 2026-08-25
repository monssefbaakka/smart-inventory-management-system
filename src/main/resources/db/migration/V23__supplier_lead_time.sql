-- How many days pass between an order being placed with a supplier and the goods arriving.
-- Nullable: a supplier recorded before this column existed, or one whose lead time nobody has
-- established, names none and its orders are placed without an expected delivery date.
ALTER TABLE suppliers ADD COLUMN lead_time_days INTEGER;

ALTER TABLE suppliers ADD CONSTRAINT ck_suppliers_lead_time_days_positive
    CHECK (lead_time_days IS NULL OR lead_time_days > 0);

-- When the goods on an order are expected to arrive: named by the buyer when the order is raised,
-- or stamped from the supplier's lead time when it is placed. Nullable, and left null on the orders
-- already placed before this column existed: what they were due is not recoverable now, and a date
-- worked out today from a lead time recorded today would be a guess dressed as a record.
ALTER TABLE purchase_orders ADD COLUMN expected_delivery_date DATE;
