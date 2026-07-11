---
name: feedback_record_field_addition_test_compat
description: When adding record components to an existing request DTO, add a backward-compatible secondary constructor instead of touching existing tests' constructor call sites
metadata:
  type: feedback
---

When a task asks to add new optional fields to an existing `record` request DTO that already has
tests calling its canonical constructor positionally (e.g. `new UpdateMealPlanSectionRequest(title,
note, callout)`), adding the fields as new trailing record components changes the canonical
constructor's arity and breaks every existing call site at compile time — including in test files
you are forbidden from modifying (this repo's mandatory TDD rule, see `CLAUDE.md` "Agent Workflow"
and the `tdd-test-guardian` agent).

**Fix:** keep the new fields as trailing record components (so `MapStruct`/new callers get them),
but add an explicit secondary constructor with the *old* arity that delegates to the canonical one
with `null` for the new fields:

```java
public record UpdateMealPlanSectionRequest(String title, String note, String callout,
        String totalsLabel, String totalsKcal, String totalsProtein) {

    public UpdateMealPlanSectionRequest(String title, String note, String callout) {
        this(title, note, callout, null, null, null);
    }
}
```

Since these DTOs implement partial-update semantics (`null` = "leave unchanged"), the old
constructor's behavior is unchanged and semantically correct — it just doesn't touch the new
fields. This lets every existing test file keep compiling byte-for-byte identical to master.

**Why:** Used in PR #218 (`feature/nutrition-meal-plan-section-totals-write`, GitHub issue: extend
`UpdateMealPlanSectionRequest` with `totalsLabel`/`totalsKcal`/`totalsProtein`). Confirmed via
`tdd-test-guardian` that `MealPlanControllerTest.java` and `MealPlanWriteServiceTest.java` — both
of which construct this DTO with the 3-arg form — produced zero diff against master, while only
new `@Test` methods were added to `MealPlanSectionWriteServiceTest.java` to cover the new fields.

**How to apply:** Whenever a task says "add field X to `SomeRequest`" and `SomeRequest` is a record
with existing test call sites using positional args, check whether adding the field as a trailing
component breaks those call sites. If so, add a secondary constructor overload preserving the old
arity rather than editing the test files. Only applies when the new fields' "unset" value is a
sensible default for old callers (true for `null` under this repo's partial-update convention,
see [[feedback_dto_db_constraint_validation]]).
