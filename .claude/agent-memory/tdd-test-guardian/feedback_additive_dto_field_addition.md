---
name: additive-dto-field-addition
description: Verification recipe confirming that adding a new field to a shared record DTO only requires additive constructor-arg edits in existing tests, not assertion changes (PR #215, nutrition meal-plan write API).
metadata:
  type: feedback
---

Confirmed clean instance (2026-07-03, branch `feature/nutrition-meal-plan-content-write-api`, PR #215):
adding a `UUID id` field to several existing record DTOs (`MealPlanSectionDTO`, `MealPlanRowDTO`,
`MealPlanStatDTO`, `MealPlanShoppingCategoryDTO`, `MealPlanShoppingItemDTO`, `MealPlanSourceDTO`,
`MealPlanChangelogEntryDTO`) to support new write endpoints required touching five pre-existing test
files (`MealPlanControllerTest`, `MealPlanMapperTest`, `MealPlanSectionAssemblerTest`,
`MealPlanServiceTest`, `MealPlanShoppingListAssemblerTest`) purely to widen constructor calls
(`new MealPlanRowDTO("a", "b", ...)` -> `new MealPlanRowDTO(UUID.randomUUID(), "a", "b", ...)`).
Every existing `assertEquals`/`assertNull`/`verify(...)` line was byte-for-byte unchanged; the diffs
were single hunks per file, matching the file's diff-stat line count, with no assertion lines inside
the changed hunks. New logic (write services, new controller endpoints) was covered exclusively by
brand-new test methods/files (`MealPlanWriteServiceTest`, `MealPlanSectionWriteServiceTest`,
`MealPlanShoppingListWriteServiceTest`, plus new `@Test` methods appended to
`MealPlanControllerTest`/`MealPlanMapperTest`).

**Why:** This is the shape of change a Java `record` DTO forces whenever a field is inserted into its
canonical constructor — every call site must be updated everywhere, including test fixtures, even
though nothing about what is being *asserted* changes. It is easy to mistake a large diff (321 lines
in `MealPlanControllerTest`) for a suspicious rewrite; the fix is to read each hunk itself, not just
the diffstat.

**How to apply:** When a PR adds a field to a shared DTO record and reports "only touched tests to add
fixture constructor args," verify by diffing each modified test file whole and checking: (1) constructor
call sites gained an argument but no `assert*`/`verify*` line's expected value changed, (2) no
`@Test` method was deleted or renamed, (3) new test files or new `@Test` methods exist for any new
logic introduced alongside the field. This is a lighter-weight sibling of
[[preapproved-formula-change]] (which covers approved *behavioral* assertion changes) — field-addition
churn does not need separate user pre-approval since no assertion is altered.
