CREATE TABLE nutrition.food
(
    id                  UUID           PRIMARY KEY,
    name                VARCHAR(255)   NOT NULL,
    brand               VARCHAR(255),
    kcal_per_100        NUMERIC(10, 2) NOT NULL,
    protein_per_100     NUMERIC(10, 2) NOT NULL,
    carbs_per_100       NUMERIC(10, 2) NOT NULL,
    fat_per_100         NUMERIC(10, 2) NOT NULL,
    fiber_per_100       NUMERIC(10, 2),
    default_serving_g   NUMERIC(10, 2),
    source              VARCHAR(20)    NOT NULL,
    creation_date       TIMESTAMP      NOT NULL,
    last_modified       TIMESTAMP      NOT NULL
);
