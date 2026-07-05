-- Rewrite meal-plan rows to be food-backed (issue #225): rows now reference a real
-- nutrition.food catalog row and have their macros derived server-side (per100 * quantityG / 100),
-- instead of carrying free-text display strings. stats, changelog and shopping-list are removed
-- entirely, as is the section/header "totals" and "shopping list" text content.
--
-- Data decision (approved as part of issue #225, no back-compat shim planned): the 10 existing
-- seeded rows are free-text (e.g. "Haferflocken, Haferdrink, Whey, TK-Himbeeren, Mandeln" as a
-- single row) and cannot be mechanically mapped 1:1 onto single Food catalog rows / real production
-- food_id values from a migration script. Since the frontend deploys in lockstep and will
-- repopulate the plan through the new write API immediately after this deploy, existing rows are
-- deleted before the new NOT NULL food-backed columns are added below.

-- Drop now-unused child tables (shopping_item before shopping_category for FK safety)
DROP TABLE IF EXISTS nutrition.meal_plan_shopping_item;
DROP TABLE IF EXISTS nutrition.meal_plan_shopping_category;
DROP TABLE IF EXISTS nutrition.meal_plan_changelog_entry;
DROP TABLE IF EXISTS nutrition.meal_plan_stat;

-- Meal-plan header: drop shopping-list text fields
ALTER TABLE nutrition.meal_plan
    DROP COLUMN shopping_list_title,
    DROP COLUMN shopping_list_note,
    DROP COLUMN shopping_list_callout;

-- Sections: drop the totals row fields
ALTER TABLE nutrition.meal_plan_section
    DROP COLUMN totals_label,
    DROP COLUMN totals_kcal,
    DROP COLUMN totals_protein;

-- Rows: delete existing free-text rows (see data decision above), then replace the old
-- display-string columns with food-backed columns.
DELETE FROM nutrition.meal_plan_row;

ALTER TABLE nutrition.meal_plan_row
    DROP COLUMN meal,
    DROP COLUMN details,
    DROP COLUMN qty,
    DROP COLUMN kcal,
    DROP COLUMN protein;

ALTER TABLE nutrition.meal_plan_row
    ADD COLUMN meal_type  VARCHAR(20)   NOT NULL,
    ADD COLUMN food_id    UUID          NOT NULL REFERENCES nutrition.food(id),
    ADD COLUMN food_name  VARCHAR(255)  NOT NULL,
    ADD COLUMN quantity_g NUMERIC(10,2) NOT NULL,
    ADD COLUMN kcal       NUMERIC(10,2) NOT NULL,
    ADD COLUMN protein_g  NUMERIC(10,2) NOT NULL,
    ADD COLUMN carbs_g    NUMERIC(10,2) NOT NULL,
    ADD COLUMN fat_g      NUMERIC(10,2) NOT NULL;

CREATE INDEX idx_meal_plan_row_food_id ON nutrition.meal_plan_row(food_id);
