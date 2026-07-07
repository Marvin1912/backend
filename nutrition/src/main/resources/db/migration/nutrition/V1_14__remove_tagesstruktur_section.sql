-- Removes the "Tagesstruktur" section (issue #227): every row it carried duplicates content that
-- already lives in the other meal-plan sections ("siehe Tagesstruktur" rows), so the section itself
-- is redundant. The frontend currently filters it out client-side as a stopgap
-- (Marvin1912/frontend#281); this migration removes it at the data layer so the API stops
-- returning it and that client-side filter can be dropped.
--
-- The section's rows are deleted first (FK safety), then the section itself, both identified by
-- the fixed seed id from V1_10. Deleting rows explicitly here (rather than relying solely on the
-- section's ON DELETE CASCADE) also covers any rows re-added under this section id via the write
-- API after V1_11 replaced the original free-text rows with food-backed ones.
DELETE FROM nutrition.meal_plan_row
WHERE meal_plan_section_id = '5b21f5d0-5dd3-4653-8941-8920f7231447';

DELETE FROM nutrition.meal_plan_section
WHERE id = '5b21f5d0-5dd3-4653-8941-8920f7231447';
