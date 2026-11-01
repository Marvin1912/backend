CREATE TABLE nutrition.excluded_day
(
    entry_date    DATE         PRIMARY KEY,
    reason        VARCHAR(255),
    creation_date TIMESTAMP    NOT NULL,
    last_modified TIMESTAMP    NOT NULL
);
