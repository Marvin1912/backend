# Memory Index

- [Food-backed schema (PR #226)](project_meal_plan_food_backed_schema.md) — rows now reference foodId+quantityG with all 4 macros; stats/changelog/shoppingList/totals removed; sources got DELETE; mealType is a 4-value enum
- [2026-07-05 row repopulation](project_meal_plan_row_repopulation_2026_07_05.md) — how the 41 rows + 3 new food entries (Olivenöl, Basmatireis, Kantine placeholder) were recreated after the migration wiped rows
- [Fat-equivalent ingredient swaps](project_fat_equivalent_swap_convention.md) — method for computing replacement qty when swapping a fat-source ingredient; qty-string steps are obsolete post food-backed rewrite
- [Food catalog lookup](project_food_catalog_lookup.md) — GET /nutrition/foods?q= for exact per-100g macros; POST to create new catalog entries (now required since every row needs a real foodId)
