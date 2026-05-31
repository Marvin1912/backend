CREATE SCHEMA IF NOT EXISTS grocery;

CREATE TABLE grocery.receipt
(
    id           UUID PRIMARY KEY,
    receipt_date DATE,
    image_content BYTEA,
    raw_ocr_text TEXT,
    creation_date TIMESTAMP NOT NULL,
    last_modified TIMESTAMP NOT NULL
);

CREATE TABLE grocery.receipt_item
(
    id         BIGSERIAL PRIMARY KEY,
    receipt_id UUID           NOT NULL REFERENCES grocery.receipt (id) ON DELETE CASCADE,
    name       VARCHAR(255)   NOT NULL,
    price      NUMERIC(10, 2) NOT NULL
);
