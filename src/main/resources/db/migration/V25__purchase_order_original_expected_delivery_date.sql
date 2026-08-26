-- What an order was promised for when it was placed, kept apart from the date it is expected on now.
-- A supplier who rings to re-promise a slipped order moves the second; the first is what they are
-- answerable for, or a supplier could talk their way out of every late delivery they ever made.
ALTER TABLE purchase_orders ADD COLUMN original_expected_delivery_date DATE;

-- The orders placed before the column existed have not been re-promised - there was no way to - so
-- the date they carry is also the date they were promised for. Recording that is a fact rather than
-- the guess a backfilled arrival date would have been.
UPDATE purchase_orders
SET original_expected_delivery_date = expected_delivery_date
WHERE expected_delivery_date IS NOT NULL;
