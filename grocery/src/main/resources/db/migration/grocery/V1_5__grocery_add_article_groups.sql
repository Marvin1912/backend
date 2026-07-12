CREATE TABLE grocery.article_group
(
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    creation_date TIMESTAMP    NOT NULL,
    last_modified TIMESTAMP    NOT NULL
);

CREATE TABLE grocery.article
(
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    normalized_name  VARCHAR(255) NOT NULL UNIQUE,
    article_group_id BIGINT REFERENCES grocery.article_group (id),
    creation_date    TIMESTAMP    NOT NULL,
    last_modified    TIMESTAMP    NOT NULL
);

ALTER TABLE grocery.receipt_item
    ADD COLUMN article_id BIGINT REFERENCES grocery.article (id);

-- Backfill: create exactly one article per distinct normalized (lower-cased, trimmed) receipt_item
-- name that already exists in the data. The lowest-id row of each group is used as the sample name.
INSERT INTO grocery.article (name, normalized_name, creation_date, last_modified)
SELECT DISTINCT ON (LOWER(TRIM(ri.name)))
    ri.name,
    LOWER(TRIM(ri.name)),
    now(),
    now()
FROM grocery.receipt_item ri
ORDER BY LOWER(TRIM(ri.name)), ri.id;

-- Link every existing receipt_item row to the article matching its normalized name.
UPDATE grocery.receipt_item ri
SET article_id = a.id
FROM grocery.article a
WHERE LOWER(TRIM(ri.name)) = a.normalized_name;
