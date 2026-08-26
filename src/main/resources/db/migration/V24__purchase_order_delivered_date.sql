-- The day the last of an order's goods arrived, stamped when a receipt completes the order. Nullable
-- while anything is still outstanding, and left null on the orders already received before this
-- column existed: the day they turned up is not recoverable now, and the day the migration ran would
-- be a guess written into the one field whose purpose is to be a fact.
ALTER TABLE purchase_orders ADD COLUMN delivered_date DATE;

-- Only a fulfilled order has a day its goods arrived. A draft, an order still awaiting delivery and
-- an abandoned one all have goods that never landed against them.
ALTER TABLE purchase_orders ADD CONSTRAINT ck_purchase_orders_delivered_when_received
    CHECK (delivered_date IS NULL OR status = 'RECEIVED');
