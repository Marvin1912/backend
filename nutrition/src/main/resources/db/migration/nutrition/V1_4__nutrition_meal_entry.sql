CREATE TABLE nutrition.meal_entry
(
    id            UUID           PRIMARY KEY,
    entry_date    DATE           NOT NULL,
    meal_type     VARCHAR(20)    NOT NULL,
    food_id       UUID           REFERENCES nutrition.food(id) ON DELETE SET NULL,
    description   VARCHAR(255),
    quantity_g    NUMERIC(10, 2),
    kcal          NUMERIC(10, 2) NOT NULL,
    protein_g     NUMERIC(10, 2) NOT NULL,
    carbs_g       NUMERIC(10, 2) NOT NULL,
    fat_g         NUMERIC(10, 2) NOT NULL,
    creation_date TIMESTAMP      NOT NULL,
    last_modified TIMESTAMP      NOT NULL
);

CREATE INDEX idx_meal_entry_entry_date ON nutrition.meal_entry(entry_date);
