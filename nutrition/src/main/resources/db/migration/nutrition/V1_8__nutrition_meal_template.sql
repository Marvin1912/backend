CREATE TABLE nutrition.meal_template
(
    id            UUID           PRIMARY KEY,
    name          VARCHAR(255)   NOT NULL,
    creation_date TIMESTAMP      NOT NULL,
    last_modified TIMESTAMP      NOT NULL
);

CREATE TABLE nutrition.meal_template_item
(
    id               UUID           PRIMARY KEY,
    meal_template_id UUID           NOT NULL REFERENCES nutrition.meal_template(id) ON DELETE CASCADE,
    food_id          UUID           NOT NULL REFERENCES nutrition.food(id) ON DELETE CASCADE,
    quantity_g       NUMERIC(10, 2) NOT NULL,
    creation_date    TIMESTAMP      NOT NULL,
    last_modified    TIMESTAMP      NOT NULL
);

CREATE INDEX idx_meal_template_item_template_id ON nutrition.meal_template_item(meal_template_id);
