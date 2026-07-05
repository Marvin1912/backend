-- Guards against a sort_order collision within a section (issue found in code review of #225):
-- computing a new row's sort_order from the *count* of existing rows breaks once a row has been
-- deleted from the middle of a section, since the next count can equal an already-used sort_order.
-- The application now derives the next sort_order from MAX(sort_order) + 1 instead of a count, and
-- this constraint is the DB-level backstop against that logic (or a concurrent insert) ever
-- silently producing two rows with the same position within a section: any collision now surfaces
-- as a clean 409 via the existing DataIntegrityViolationException handling in
-- NutritionExceptionHandler, instead of silently corrupting the displayed row order.
ALTER TABLE nutrition.meal_plan_row
    ADD CONSTRAINT meal_plan_row_section_sort_order_key UNIQUE (meal_plan_section_id, sort_order);
