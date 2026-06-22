CREATE TABLE nutrition.sport_activity
(
    id            UUID           PRIMARY KEY,
    entry_date    DATE           NOT NULL,
    activity_type VARCHAR(20)    NOT NULL,
    description   VARCHAR(255),
    kcal_burned   NUMERIC(10, 2) NOT NULL,
    creation_date TIMESTAMP      NOT NULL,
    last_modified TIMESTAMP      NOT NULL
);

CREATE INDEX idx_sport_activity_entry_date ON nutrition.sport_activity(entry_date);
