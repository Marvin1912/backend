---
name: project-meal-plan-food-backed-schema
description: As of PR #226 (2026-07-05, issue #225) the meal-plan API was rewritten to food-backed rows; several previously-memoed DTO shapes/endpoints (stats, changelog, shoppingList, free-text row fields) no longer exist
metadata:
  type: project
---

`MealPlanController` (`nutrition/src/main/java/com/marvin/nutrition/controller/MealPlanController.java`)
was rewritten in PR #226 (merged 2026-07-05, GitHub issue #225). Confirm the current shape by reading
the controller/DTOs directly before relying on older memories — this rewrite invalidated several
things this agent used to know:

- **Rows are food-backed, not free text.** `MealPlanRowDTO` now carries `id`, `mealType`, `foodId`,
  `foodName` (snapshot), `quantityG`, `kcal`, `proteinG`, `carbsG`, `fatG` — all four macros are
  present and server-derived from the referenced food's per-100g values × `quantityG`
  (`HALF_UP`, 2 decimals). The old free-text `meal`/`details`/`qty` string fields are gone.
- **`mealType` is a 4-value enum only**: `BREAKFAST`, `LUNCH`, `DINNER`, `SNACK`
  (`com.marvin.nutrition.entity.MealType`) — there is no separate enum literal per German label.
  When source content uses German meal-slot names, map them onto these 4 values, e.g. "Frühstück"
  -> `BREAKFAST`, "Mittag"/"Mittagessen" -> `LUNCH`, "Nachmittag" -> `SNACK`, "Abendessen" -> `DINNER`.
  Two visually-different German slot names in different sections can both be `LUNCH`.
- **Row write endpoints**: `POST /nutrition/meal-plan/sections/{sectionId}/rows` (single, 201 +
  Location) and `POST /nutrition/meal-plan/sections/{sectionId}/rows/batch` (`{ "rows": [...] }`,
  max 50, whole-batch rollback on any unknown food, 201 returns a JSON array) both take
  `{ mealType, foodId, quantityG }`. `PUT /rows/{id}` takes `{ mealType?, foodId?, quantityG? }` and
  re-snapshots macros. `DELETE /rows/{id}` also now exists (204).
- **Stats, changelog, and shoppingList are gone entirely** — their own tables/DTOs/services/
  repositories were deleted, along with section/header "totals" and shopping-list text fields. Any
  memory referencing `PUT /stats/{id}`, `POST /changelog`, `PUT /shopping-categories|items/{id}`, or
  a section's `totals` field is now obsolete — those endpoints/fields do not exist in the controller
  as of this rewrite.
- **Sources now support `DELETE`**: `DELETE /nutrition/meal-plan/sources/{id}` (204) exists in
  addition to `PUT`. The old "sources have no delete" gap is resolved.
- Sections themselves are still fixed/seeded (no section create/delete).

**Why**: this agent's memory previously described the pre-rewrite API (free-text rows with
kcal/protein only, a stats/changelog/shoppingList surface). Acting on those stale specifics against
the current controller would produce 400s (wrong field names) or silently target endpoints that no
longer exist.

**How to apply**: before any write against `/nutrition/meal-plan/*`, re-verify the exact DTO shape by
reading `MealPlanController.java` and the relevant `dto/*.java` files fresh — don't trust a cached
field list for this module without a quick re-read, since it has already changed shape once. See
[[project_meal_plan_row_repopulation_2026_07_05]] for the concrete repopulation this schema enabled.
