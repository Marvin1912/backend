---
name: project-meal-plan-row-repopulation-2026-07-05
description: PR #226's migration deleted all 10 original free-text meal-plan rows with a documented plan to repopulate via the new food-backed write API; that repopulation was done 2026-07-05 (41 rows across the 3 seeded sections)
metadata:
  type: project
---

`V1_11__meal_plan_food_backed_rows.sql` (part of PR #226, issue #225) intentionally deleted all
original free-text `nutrition.meal_plan_row` rows because they couldn't be mechanically mapped onto
the new food-backed schema (see [[project_meal_plan_food_backed_schema]]). The 3 parent sections
survived untouched (same ids): "1 · Tagesstruktur" (`5b21f5d0-5dd3-4653-8941-8920f7231447`),
"2 · Wochentage (Mo–Do)" (`41de111d-8e9e-4067-98ed-6d6ccba19c07`), "3 · Wochenende (Fr–So)"
(`edc7ee00-af02-4c76-850c-38315022f2b7`).

On 2026-07-05 the original content (recovered from the git history of the superseded
`V1_10__nutrition_meal_plan.sql`) was recreated through `POST .../rows/batch`, splitting each old
combined free-text row into one food-backed row per ingredient. Result: 8 rows in section 1
(2 meal-type groups: BREAKFAST, SNACK), 15 in section 2, 18 in section 3 (4 meal-type groups each:
BREAKFAST, LUNCH, SNACK, DINNER) — 41 total. Recomputed weekly aggregates landed close to the
original documented targets (weekday ~2418 kcal/181.5g protein vs original ~2407/182.2; weekend
~2438 kcal/184.3g protein vs original ~2428/186.5) — small deviations from unit-conversion/placeholder
choices below, not a wildly-off recompute.

Three food catalog entries didn't exist yet and were created via `POST /nutrition/foods` before use:
- **Olivenöl** (Lidl, `source: MANUAL`) — 884 kcal / 0 protein / 0 carbs / 100 fat per 100g (standard
  pure-olive-oil reference values, not a photographed/barcode entry).
- **Basmatireis** (Lidl, roh, `source: MANUAL`) — 349 kcal / 7.6 protein / 77 carbs / 0.6 fat per
  100g (matches the fatsecret.de/Lidl citation from the original migration's footer source).
- **"Kantine Mittagessen (Richtwert)"** (`source: ESTIMATE`, no brand) — a placeholder food for the
  weekday canteen lunch (no real fixed ingredient list), back-calculated so `quantityG=100`
  reproduces ~650 kcal / 40g protein: `kcalPer100=650, proteinPer100=40, carbsPer100=45,
  fatPer100=22`. Note the carbs/fat split does **not** Atwater-reconcile with the kcal figure
  (40×4 + 45×4 + 22×9 = 538, not 650) — `FoodDTO`'s `kcalPer100` is an independent field, not
  derived from the other three, consistent with how other catalog entries work; this was an
  explicit instruction to "round out" the macros, not a derivation error.

Unit conversions applied from the original "1 Stk" quantities: Ei = 50g, Banane = 120g (matches
`defaultServingG` already on those catalog entries).

**Why**: explains why the currently-visible 41 meal-plan rows and 3 "new-looking" food catalog
entries exist with no corresponding Flyway seed data — they were created purely through the write
API as a data-recovery operation, not a migration.

**How to apply**: if asked to further edit any of these rows (e.g. "swap the olive oil for
something else", "the canteen lunch estimate is wrong"), don't re-derive foodId/quantityG from
scratch — look them up fresh via `GET /nutrition/meal-plan` (rows) or `GET /nutrition/foods?q=...`
(catalog), per [[project_food_catalog_lookup]]. If the Kantine placeholder's macro split ever needs
adjusting, remember it's an independent-field entry, not Atwater-derived.
