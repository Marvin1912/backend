-- Adds support for alternative/OR meal options within a single meal-plan slot (e.g. a weekend
-- SNACK slot offering "Protein Käsekuchen ODER Protein Brownie"): the user picks whichever
-- alternative day-to-day, but both rows exist in the plan side by side. Rows that belong to the
-- same alternative choice share an `alternative_group_id`; rows without one are standalone as
-- before. This mirrors the module's existing fat-equivalent-swap convention, but for options that
-- are meant to coexist as choices rather than replace one another.
--
-- The backend's role is limited to persisting and returning this grouping key per row — the
-- "pick the max-kcal alternative as a safety-margin ceiling and sum only its macros into the
-- section total" computation is a frontend-only concern (totals were deliberately removed from
-- this backend in migration V1_11).
--
-- Nullable, no backfill: existing rows stay NULL (ungrouped), which is fully backward compatible.
ALTER TABLE nutrition.meal_plan_row
    ADD COLUMN alternative_group_id UUID NULL;

-- Partial index mirroring the idx_meal_plan_row_food_id precedent from V1_11: only rows that
-- actually belong to a group are ever looked up by this key (see
-- MealPlanRowRepository#findAllByAlternativeGroupId), so indexing the NULL majority would be pure
-- overhead.
CREATE INDEX idx_meal_plan_row_alternative_group_id
    ON nutrition.meal_plan_row(alternative_group_id)
    WHERE alternative_group_id IS NOT NULL;
