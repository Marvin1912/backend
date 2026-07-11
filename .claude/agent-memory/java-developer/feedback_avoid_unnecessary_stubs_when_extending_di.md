---
name: avoid-unnecessary-stubs-when-extending-di
description: When adding new constructor dependencies to a service, check if Mockito's default return value already satisfies an existing test before adding stubs to it — unstubbed primitive-returning mocks default sensibly and let you leave the test byte-identical
metadata:
  type: feedback
---

When a service under test gains new constructor-injected dependencies (e.g. adding
`MealPlanRowRepository`/`MealTemplateItemRepository` to `FoodService` for issue #225's food-delete
referential-integrity guard), it's tempting to "fix" every existing `@InjectMocks`-based test by
adding `when(newMock.someMethod(...)).thenReturn(...)` stubs so the new code path behaves as
expected. Don't do this reflexively — check whether Mockito's default return value already produces
the right behavior first.

**Concretely**: an unstubbed mock method returning a primitive `long`/`int`/etc. returns `0` by
default (not an exception, not `null`). If the new logic branches on "count == 0 means not
referenced, proceed", an existing happy-path test needs zero changes — the default `0` from two
unstubbed `countByFoodId(...)` calls already means "unreferenced" and the original test still passes
unmodified.

**Why this matters here**: this repo's TDD workflow mandates the `tdd-test-guardian` agent flag any
modification to an *existing* test method, even a "safe" one like adding stubs with no assertion
change, and stop to ask before proceeding. Realizing the stubs were unnecessary (verified by reverting
them and re-running the test — still green) let the existing test stay byte-identical to master,
avoiding an unnecessary approval round-trip and keeping the diff smaller.

**How to apply**: When the guardian (or your own diff review) flags a modified existing test, ask "is
this modification actually required for the test to still pass, or would the mock's default behavior
already cover it?" before asking the user for approval — reverting an unnecessary change is strictly
better than getting sign-off on it.
