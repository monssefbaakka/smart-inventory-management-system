-- How much of an ordered line has actually turned up. New lines start at nothing; the orders already
-- marked RECEIVED were received in full by the only mechanism that existed, so their lines are
-- backfilled to the quantity ordered rather than left claiming nothing arrived.
ALTER TABLE purchase_order_items
    ADD COLUMN received_quantity INTEGER NOT NULL DEFAULT 0;

UPDATE purchase_order_items i
SET received_quantity = i.quantity
FROM purchase_orders o
WHERE o.id = i.purchase_order_id
  AND o.status = 'RECEIVED';

-- A receipt may fall short of what was ordered but never pass it: an overage is not a receipt
-- against the order, it is stock the order did not buy.
ALTER TABLE purchase_order_items
    ADD CONSTRAINT ck_purchase_order_items_received_within_ordered
        CHECK (received_quantity >= 0 AND received_quantity <= quantity);
