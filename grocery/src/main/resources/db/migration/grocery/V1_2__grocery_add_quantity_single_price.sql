ALTER TABLE grocery.receipt_item
    ADD COLUMN single_price NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN quantity     INTEGER        NOT NULL DEFAULT 1;
