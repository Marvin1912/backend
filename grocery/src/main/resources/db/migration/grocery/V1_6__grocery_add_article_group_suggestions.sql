CREATE TABLE grocery.article_group_suggestion
(
    id                 BIGSERIAL PRIMARY KEY,
    article_id         BIGINT           NOT NULL REFERENCES grocery.article (id),
    suggested_group_id BIGINT           NOT NULL REFERENCES grocery.article_group (id),
    score              DOUBLE PRECISION NOT NULL,
    source             VARCHAR(20)      NOT NULL,
    status             VARCHAR(20)      NOT NULL,
    creation_date      TIMESTAMP        NOT NULL,
    last_modified      TIMESTAMP        NOT NULL
);

CREATE INDEX idx_article_group_suggestion_status ON grocery.article_group_suggestion (status);

-- at most one PENDING suggestion per article at a time
CREATE UNIQUE INDEX uq_article_group_suggestion_pending_article
    ON grocery.article_group_suggestion (article_id) WHERE status = 'PENDING';
