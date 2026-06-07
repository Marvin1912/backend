CREATE TABLE nutrition.profile
(
    id               BIGSERIAL PRIMARY KEY,
    sex              VARCHAR(10)    NOT NULL,
    birth_date       DATE           NOT NULL,
    height_cm        NUMERIC(5, 1)  NOT NULL,
    activity_level   VARCHAR(20)    NOT NULL,
    goal             VARCHAR(10)    NOT NULL,
    protein_per_kg   NUMERIC(4, 2)  NOT NULL DEFAULT 2.0,
    fat_pct          NUMERIC(4, 2)  NOT NULL DEFAULT 0.30,
    basal_kcal       INT,
    creation_date    TIMESTAMP      NOT NULL,
    last_modified    TIMESTAMP      NOT NULL
);

CREATE TABLE nutrition.weight_entry
(
    id            BIGSERIAL PRIMARY KEY,
    entry_date    DATE           NOT NULL UNIQUE,
    weight_kg     NUMERIC(5, 2)  NOT NULL,
    creation_date TIMESTAMP      NOT NULL,
    last_modified TIMESTAMP      NOT NULL
);
