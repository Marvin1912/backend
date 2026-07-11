---
name: checkstyle-param-limit-service-split
description: When a write-service needs 8+ collaborators (one JpaRepository per entity type + mapper), split into narrower delegate services rather than exceeding checkstyle's 7-param constructor limit
metadata:
  type: feedback
---

Checkstyle's `ParameterNumber` check (max=7, see `.claude/rules/checkstyle.md`) applies to
constructors too. A single facade service that needs one `JpaRepository` per entity type it writes to
(plus a mapper, plus e.g. a `FoodRepository` for a lookup) can easily exceed 7 params once the domain
has more than a handful of related entity tables.

**Solution used successfully** (in `nutrition`'s meal-plan write API, see
[[project_meal_plan_write_api]]): split the facade into narrower `@Service` delegate beans, each
holding only the repositories for a cohesive sub-group of entities (mirroring any existing read-side
split, e.g. this module's `MealPlanSectionAssembler`). The facade (`MealPlanWriteService`) keeps the
repositories it can hold within budget directly, and takes the delegate bean(s) as its remaining
constructor params — still landing at ≤7 total. The facade's delegating methods for those entity
types are plain pass-throughs (no `@Transactional` needed on the facade method itself — the delegate
bean's own `@Transactional` method establishes the transaction boundary correctly since it's a genuine
cross-bean call, so Spring's AOP proxy still applies).

**Why**: First discovered building the original meal-plan write API (PR #215) — a flat
`MealPlanWriteService` with one repository per entity type would have exceeded the limit. Confirmed
again in issue #225's rewrite: adding a `FoodRepository` dependency to `MealPlanSectionWriteService`
(for row food-lookups) stayed comfortably within budget specifically because the delegate-split
pattern was already in place — the facade's own constructor never had to grow.

**How to apply**: Before adding another dependency to any service constructor, check if it's about to
exceed 7 params. If the extra collaborators naturally group into a read-side split that already
exists in the module (assemblers, sub-services), reuse that same grouping for the write side rather
than inventing a new one.
