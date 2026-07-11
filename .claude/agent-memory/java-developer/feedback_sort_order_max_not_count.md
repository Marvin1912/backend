---
name: sort-order-max-not-count
description: Never derive a new row's display-position/sort_order from count() of existing siblings — use max(existing sort_order) + 1, since count() collides with a remaining sibling's value once anything has been deleted from the middle
metadata:
  type: feedback
---

Caught in code review of PR #226 (issue #225's meal-plan rows): `MealPlanSectionWriteService.addRow`
computed a new row's `sortOrder` as `mealPlanRowRepository.findAllByMealPlanSectionIdOrderBySortOrderAsc(sectionId).size()`.
This is correct only as long as no row has ever been deleted from the section. Once one is (e.g.
rows at 0, 1, 2 → delete the row at 1 → remaining rows at 0, 2), `size()` is 2, so the next insert
gets `sortOrder = 2` — colliding with the row already there. `ORDER BY sort_order ASC` has no
tiebreaker, so display order becomes ambiguous with no error raised anywhere; it's a silent
corruption bug, not a crash.

**Fix applied**: derive the next value from `max(existing sort_order) + 1` instead — a repository
method like `findFirstBy<Scope>OrderBySortOrderDesc(...): Optional<Entity>`, mapped to
`.map(e -> e.getSortOrder() + 1).orElse(0)`. Also added a DB-level unique constraint on
`(section_id, sort_order)` via migration as a backstop: it turns any remaining edge case (a bug
elsewhere, or a genuine concurrent-insert race) into a clean `DataIntegrityViolationException` → 409
via the existing `NutritionExceptionHandler` convention, instead of silent duplicate positions.

**Why this matters generally**: `count()`/`.size()` and `max() + 1` are NOT interchangeable for
computing "the next position in a sequence" the moment deletions are possible — they only agree when
the sequence has never had a gap. Any codebase pattern of "assign new item's position/order/index as
the count of existing items" is a latent version of this exact bug if items in that sequence can ever
be deleted.

**How to apply**: When writing or reviewing any "append to an ordered list of DB rows" logic (sort
order, display position, sequence number), check whether items in that list can be deleted. If yes,
always derive the next value from `MAX(existing) + 1`, never from a count, and prefer adding a DB
unique constraint on the (scope, position) pair as a cheap backstop.
