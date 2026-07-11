---
name: project-food-catalog-lookup
description: GET /nutrition/foods?q=... exposes per-100g macros for catalogued ingredients; POST creates new entries — since PR #226 (food-backed rows) every meal-plan row must reference a real foodId from this catalog
metadata:
  type: project
---

The `nutrition` module has its own food catalog API,
`nutrition/src/main/java/com/marvin/nutrition/controller/FoodController.java`, mounted at
`/nutrition/foods`. `GET /nutrition/foods?q=<name>` (case-insensitive contains) returns matching
catalog entries with `id`, `kcalPer100`, `proteinPer100`, `carbsPer100`, `fatPer100` (and sometimes
`fiberPer100`, `defaultServingG`, `source`). This is the actual "bestehende Lebensmitteldatenbank des
Nutrition-Trackings" referenced in the meal plan's `footer.note` — i.e. when the footer/callout
text says an ingredient's values "stammen aus der bestehenden Lebensmitteldatenbank", its exact
per-100g numbers live here and can be looked up directly rather than guessed or asked about.

**Since PR #226 (2026-07-05, see [[project_meal_plan_food_backed_schema]]), this is no longer just a
reference lookup** — every `MealPlanRowDTO` now references a real `foodId` here, so adding/editing a
meal-plan row (not just computing a swap) requires either finding an existing catalog `id` via `q=`
or, if the ingredient isn't catalogued, `POST /nutrition/foods` (`{ name, brand?, kcalPer100,
proteinPer100, carbsPer100, fatPer100, fiberPer100?, defaultServingG?, source? }`, `source` one of
`MANUAL`/`PHOTO`/`ESTIMATE`/`BARCODE`, defaults to `MANUAL`) to create one first. Placeholder/
back-calculated entries (no fixed real ingredient, e.g. a canteen-lunch estimate) should use
`source: ESTIMATE`; independently-researched reference values (e.g. standard olive-oil composition)
use `MANUAL`. Note `kcalPer100` is stored independently of the other three macros — it is **not**
required to Atwater-reconcile with protein/carbs/fat×4/4/9 (confirmed 2026-07-05, "Kantine
Mittagessen (Richtwert)" placeholder: 650 kcal but 40P/45C/22F only sums to 538 via Atwater — this is
by design, not a bug, when the kcal figure is independently sourced/estimated).

Confirmed values (2026-07-04, Rinderhüftsteak-portion + Basmatireis->Dinkelnudeln task):
- Frisches Rinderhüftsteak (Lidl): 104 kcal / 21 g protein / 0 g carbs / 2.2 g fat per 100 g
- Kaisergemüse (Freshona): 41 kcal / 2.4 g protein / 5 g carbs / 0.5 g fat per 100 g
- Walnusskerne (Lidl): 712 kcal / 15.5 g protein / 3.7 g carbs / 69.1 g fat per 100 g (matches the
  value already cited in-plan from the earlier Olivenöl->Walnuss changelog entry)
- Bio Dinkel Penne (Lidl/Bioland): 360 kcal / 11 g protein / 74 g carbs / 1.5 g fat per 100 g — this
  is a **raw/dry pasta** value, consistent with the plan's convention of weighing grains/pasta/rice
  dry before cooking (shopping list explicitly labels rice "roh"); do not confuse with cooked-pasta
  reference values from external sources, which are ~2.5x lower per gram.
- Hähnchenbrust: 104 kcal / 22 g protein / 0 g carbs / 1.8 g fat per 100 g
- Whey Konzentrat Erdbeere: 378 kcal / 75.1 g protein / 5.9 g carbs / 5.9 g fat per 100 g
- Ei: 90 kcal / 7.5 g protein / 0.5 g carbs / 6.5 g fat per 100 g (defaultServingG 50)
- Bio Haferdrink: 31 kcal / 0.5 g protein / 3.6 g carbs / 1.4 g fat per 100 g
- Bio Hafervollkornflocken Kleinblatt: 371 kcal / 13.2 g protein / 59.5 g carbs / 6.7 g fat per 100 g
- Himbeeren (REWE Beste Wahl): 38 kcal / 0.8 g protein / 5.1 g carbs / 0.5 g fat per 100 g
- Blanchierte Mandeln gehackt (Lidl): 619 kcal / 24 g protein / 5.7 g carbs / 53 g fat per 100 g
- Speisequark Magerstufe (Milbona) — "Magerquark": 68 kcal / 11.8 g protein / 4 g carbs / 0.3 g fat
  per 100 g (contrary to the earlier "not catalogued" note below — it was added to the catalog at
  some point after 2026-07-04; always re-check with a fresh `q=` search rather than trusting a stale
  "not catalogued" list)
- Banane: 93 kcal / 1 g protein / 20 g carbs / 0.2 g fat per 100 g (`source: MANUAL`)
- Olivenöl (Lidl, created 2026-07-05, `source: MANUAL`): 884 kcal / 0 protein / 0 carbs / 100 fat
  per 100 g
- Basmatireis (Lidl, roh, created 2026-07-05, `source: MANUAL`): 349 kcal / 7.6 g protein / 77 g
  carbs / 0.6 g fat per 100 g
- "Kantine Mittagessen (Richtwert)" (created 2026-07-05, `source: ESTIMATE`, no brand): 650 kcal /
  40 g protein / 45 g carbs / 22 g fat per 100 g — placeholder for the weekday canteen lunch, not a
  real ingredient (see [[project_meal_plan_row_repopulation_2026_07_05]])

Historical note: as of 2026-07-04, Magerquark/Basmatireis/Banane were *not yet* catalogued — that has
since changed (Magerquark and Banane were added independently of this agent's work; Basmatireis and
Olivenöl were created by this agent on 2026-07-05). Don't trust a "not catalogued" finding from an
older memory without a fresh `q=` check — the catalog grows over time.

**How to apply:** before asking the user for reference macros on an ingredient swap or portion-size
correction, first try `GET /nutrition/foods?q=<name>` — if it returns a hit, use those numbers
directly (isolate the single ingredient's contribution to a row rather than trying to back-solve
multiple unknowns from the row's aggregate kcal/protein). Only fall back to asking the user when the
catalog has no entry (as with Magerquark/Basmatireis/Banane) and no other unambiguous source is at
hand. See [[project_fat_equivalent_swap_convention]] for the general swap-computation method this
complements.
