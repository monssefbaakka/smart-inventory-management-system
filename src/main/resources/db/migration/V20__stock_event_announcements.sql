-- The low-stock condition each shelf last announced to the notification channels. A shortage is
-- announced when it starts or deepens, so what was said last time has to outlive the movement that
-- said it. Null means nothing stands: the shelf is comfortable, or it was already low when this
-- column arrived and the next movement that finds it low announces once.
ALTER TABLE products ADD COLUMN announced_stock_event VARCHAR(16);

ALTER TABLE products ADD CONSTRAINT ck_products_announced_stock_event
    CHECK (announced_stock_event IS NULL OR announced_stock_event IN ('LOW_STOCK', 'OUT_OF_STOCK'));

-- The same, for a warehouse measured against the reorder point it holds for the product. Kept per
-- level so one site falling quiet says nothing about another, or about the product total.
ALTER TABLE stock_levels ADD COLUMN announced_stock_event VARCHAR(16);

ALTER TABLE stock_levels ADD CONSTRAINT ck_stock_levels_announced_stock_event
    CHECK (announced_stock_event IS NULL OR announced_stock_event IN ('LOW_STOCK', 'OUT_OF_STOCK'));
