-- Adds an explicit day-count to each meal-plan section. Sections like "2 · Wochentage (Montag –
-- Donnerstag)" represent several calendar days of the week, not one, but the shopping-list
-- aggregation only ever summed each row's quantityG once per section regardless of how many days
-- it actually spans -- undercounting total quantities needed for the week (e.g. a 300 g/day row in
-- a 4-day section needs 1200 g bought, not 300 g). day_count lets that calculation scale each
-- section's row quantities by the number of days it applies to.
ALTER TABLE nutrition.meal_plan_section
    ADD COLUMN day_count INTEGER NOT NULL DEFAULT 1;

-- Correct the day counts for the currently seeded sections (identified by title, since sections
-- have no other stable natural key here).
UPDATE nutrition.meal_plan_section SET day_count = 7 WHERE title LIKE '%Tagesstruktur%';
UPDATE nutrition.meal_plan_section SET day_count = 4 WHERE title LIKE '%Wochentage%';
UPDATE nutrition.meal_plan_section SET day_count = 3 WHERE title LIKE '%Wochenende%';
