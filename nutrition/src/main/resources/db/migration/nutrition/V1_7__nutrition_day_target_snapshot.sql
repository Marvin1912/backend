CREATE TABLE nutrition.day_target_snapshot
(
    entry_date       DATE           PRIMARY KEY,
    bmr              INT            NOT NULL,
    maintenance_kcal INT            NOT NULL,
    target_kcal      INT            NOT NULL,
    protein_g        INT            NOT NULL,
    fat_g            INT            NOT NULL,
    carbs_g          INT            NOT NULL,
    basis            VARCHAR(20)    NOT NULL,
    creation_date    TIMESTAMP      NOT NULL,
    last_modified    TIMESTAMP      NOT NULL
);
