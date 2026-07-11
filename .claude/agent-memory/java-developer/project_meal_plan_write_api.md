---
name: meal-plan-write-api
description: nutrition module's meal-plan content is editable via REST endpoints; rows are food-backed as of issue #225 (stats/changelog/shopping-list removed)
metadata:
  type: project
---

`MealPlanController` (`nutrition/src/main/java/com/marvin/nutrition/controller/MealPlanController.java`)
exposes write endpoints for the weekly meal-plan reference document, in addition to
`GET /nutrition/meal-plan`:

```
PUT    /nutrition/meal-plan                                    (header: eyebrow/title/description/footerNote)
PUT    /nutrition/meal-plan/sections/{id}                      (title/note/callout)
POST   /nutrition/meal-plan/sections/{sectionId}/rows           (mealType/foodId/quantityG) -> 201 + Location
POST   /nutrition/meal-plan/sections/{sectionId}/rows/batch     (rows: [...]) -> 201, array, whole-batch rollback on unknown food
PUT    /nutrition/meal-plan/rows/{rowId}                        (mealType?/foodId/quantityG) -> re-snapshots macros
DELETE /nutrition/meal-plan/rows/{rowId}                        -> 204
PUT    /nutrition/meal-plan/sources/{id}                        (label/url)
DELETE /nutrition/meal-plan/sources/{id}                        -> 204
```

**As of issue #225 (PR #226, 2026-07-05): rows are food-backed, not free text.** Each row references a
real `nutrition.food` catalog row (`foodId`) and its `kcal`/`proteinG`/`carbsG`/`fatG` are derived
server-side from that food's per-100g values and the row's `quantityG`
(`per100.multiply(quantityG).divide(HUNDRED, 2, RoundingMode.HALF_UP)` — same snapshot pattern as
`MealEntryWriteService`/`MealTemplateService`). The old free-text `meal`/`details`/`qty`/`kcal`/`protein`
string columns, plus `stats`, `changelog`, and `shoppingList` (their own tables, DTOs, services,
repositories) and section/header "totals"/shopping-list text fields, were **removed entirely** — this
was a deliberate breaking rewrite with no back-compat shim (frontend deploys in lockstep from a
separate repo). Sections themselves stay fixed/seeded; there is no section create/delete.

**Referential integrity**: `DELETE /nutrition/foods/{id}` (`FoodController`/`FoodService`) now returns
**409** with body `{ "referencedBy": { "mealPlanRows": <n>, "mealTemplateItems": <n> } }` if the food is
still referenced by any `MealPlanRowEntity` or `MealTemplateItemEntity`, via a `FoodReferencedException`
handled by `NutritionExceptionHandler` (see [[nutrition-exception-handler-convention]]).

**Why**: The user previously rejected both a new Flyway migration and direct `psql` UPDATE against the
live DB as ways to fix meal-plan content — content corrections belong in this write API, not
migrations/manual SQL (still true for header/section/source text, and now for rows too, though rows
are edited as structured food+quantity data rather than free text).

**How to apply**: A request to "fix a typo in the meal plan" or "add a row for lunch on Tuesday" should
go through these endpoints (or a client built on them, e.g. the `nutrition-meal-plan-editor` agent),
never a migration or manual SQL. See [[checkstyle-param-limit-service-split]] for the service-layer
design (`MealPlanWriteService` facade + `MealPlanSectionWriteService` delegate) used to keep write-side
constructors within checkstyle's parameter limit while adding the food-lookup dependency this rewrite needed.

**Removing a whole section is out of scope for the write API** (there is no `DELETE /sections/{id}`,
by design — "sections themselves stay fixed/seeded" above). Issue #227 (PR #230, 2026-07-07) removed
the redundant "Tagesstruktur" section this way: a *new* Flyway migration (`V1_14`) with `DELETE`
statements, not an edit to the original `V1_10` seed migration (already applied in prod; editing it
risks a checksum/validation mismatch) and not a new REST endpoint.

**Trap when writing a migration against "seeded" row/section ids**: V1_11 (issue #225) already ran
`DELETE FROM nutrition.meal_plan_row` unconditionally (no `WHERE`) to wipe the old free-text rows
before adding the food-backed NOT NULL columns — so any row UUID hardcoded in V1_10's seed data no
longer exists in prod today; only the *section* ids/rows from V1_10 survived that wipe. A migration
that needs to touch "seed data" rows must delete by a durable key (e.g. `meal_plan_section_id =
'<section-uuid>'`) rather than assuming the original V1_10 row UUIDs are still present — check every
later migration in the chain for bulk deletes/rewrites before assuming seed-data ids are stable.
