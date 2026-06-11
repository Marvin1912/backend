ALTER TABLE nutrition.meal_entry ADD COLUMN food_name VARCHAR(255);

UPDATE nutrition.meal_entry me
SET food_name = f.name
FROM nutrition.food f
WHERE me.food_id = f.id;
