---
name: project-fat-equivalent-swap-convention
description: Convention for computing replacement qty/kcal/protein when one fat-source ingredient in a meal-plan row is swapped for another (e.g. Olivenöl -> Walnusskerne)
metadata:
  type: project
---

**Note (2026-07-05): mechanics changed with the food-backed row rewrite (PR #226, see
[[project_meal_plan_food_backed_schema]]).** Rows no longer carry a free-text `qty` string or their
own directly-editable `kcal`/`protein` fields — a swap is now done via `PUT /rows/{id}` with a new
`foodId` and/or `quantityG`; `kcal`/`proteinG`/`carbsG`/`fatG` are re-derived server-side from the
referenced food automatically on every write. Steps 1-2 and 5 below (getting reference macros,
matching fat grams to compute the new quantity, tracking effects across every touched row) are still
the right *method*; steps 3-4's qty-*string* formatting/rounding conventions ("compact vs spaced",
manually recomputing a kcal/protein delta) are obsolete for rows — just round `quantityG` sensibly
(e.g. one decimal) and let the server compute the snapshot macros. Shopping-list items (step 6) no
longer exist at all (removed in the same rewrite), so that step is fully obsolete.

When the user asks to replace one ingredient with another as a fat-equivalent swap (e.g. "10 g
Olivenöl" -> "Walnusskerne"), the pattern confirmed by the user (2026-07-03, Olivenöl ->
Walnusskerne task, pre-rewrite — re-read for the current row-write mechanics) is:

1. Get the reference macros for both ingredients (old ingredient may not be catalogued — user
   supplies a standard reference value per 100 g, e.g. pure olive oil = 884 kcal / 100 g fat / 0 g
   protein / 0 g carbs per 100 g).
2. Compute the new quantity by matching **fat grams**, not total weight or kcal:
   `new_qty_g = old_fat_g / (new_ingredient_fat_g_per_100g / 100)`.
3. Round the new quantity to one decimal (matches existing qty precision elsewhere, e.g. "14,5g")
   and use it consistently across all affected rows/qty strings — compact style `NNg` (no space)
   inside a row's `qty` field (e.g. `"170g/120g/200g/14,5g/100g/1 Stk"`), but `"145 g"` (with
   space) in shopping-item `qty` (matches existing convention: rows are compact, shopping items use
   a space before the unit).
4. Recompute kcal/protein deltas using the *rounded* qty, not the exact fat-matching qty — the
   rounding itself introduces a small residual (here: rounding 14.4718 g up to 14.5 g overshot fat
   by +0.0195 g per serving). Always compute this residual and report whether it's large enough to
   move any *displayed* figure after rounding (see [[feedback_narrow_scope_header_stats]]) — in
   this case it summed to ~+0.03 g/day averaged over the week, which did **not** change any
   displayed value ("52,3 g" and "~52 g" stayed the same), so no stat/callout edit was made for
   fat, only reported to the user.
5. Section totals (`totalsKcal`/`totalsProtein`) get the sum of deltas for every row in that
   section that was touched (a section can have the swap appear more than once, e.g. lunch AND
   dinner both containing olive oil in the "Wochenende" section — double the per-row delta for that
   section's total).
6. Shopping-list item qty = per-serving new qty × number of weekly servings, which must be derived
   from the plan structure (count how many rows across all sections reference the ingredient), not
   guessed. Sanity-check against the old shopping-item qty (old qty ÷ old per-serving qty should
   equal the same serving count).
