---
name: feedback_dto_db_constraint_validation
description: New write-request DTOs must mirror DB column constraints (VARCHAR length, CHECK) with Bean Validation from the start
metadata:
  type: feedback
---

When adding a new REST write-request DTO (record) backed by a JPA entity/DB column, add Bean
Validation constraints that mirror the column's constraints immediately — don't rely on the DB to
reject bad input.

- `@Size(max = N)` on every field backed by a bounded `VARCHAR(N)` column (leave unbounded `TEXT`
  columns unconstrained).
- `@Pattern(regexp = "...")` (or an enum type) for any column with a DB `CHECK (col IN (...))`
  constraint — a length limit alone is not enough to enforce an allowed-values set.
- These coexist fine with the module's `@NullOrNotBlank` custom constraint (see
  `nutrition/src/main/java/com/marvin/nutrition/dto/validation/NullOrNotBlank.java`): it only
  rejects blank, `@Size`/`@Pattern` only validate non-null values, so a field can carry all three
  without conflict.

**Why:** PR #215 added the meal-plan write API without this, so oversized/malformed input reached
the DB and surfaced as a generic `DataIntegrityViolationException` → 409 (via
`NutritionExceptionHandler.handleDataIntegrityViolation`) instead of a clean 400 with a field-level
message. `java-code-reviewer` flagged it as non-blocking, and it was fixed after merge in PR #216
(`fix/nutrition-meal-plan-request-validation`) — 22 new failing tests before the fix confirmed the
gap. Doing it up front avoids a follow-up PR.

**How to apply:** Whenever writing a new `Create*Request`/`Update*Request` record for this repo,
check the corresponding entity's `@Column(length = ...)` and the owning module's Flyway migration
for `VARCHAR(N)` / `CHECK` constraints, and add the matching annotations in the same PR as the DTO.
Reference pattern: `CreateSportActivityRequest.description` uses `@Size(max = 255)` mirroring its
column length — follow that convention.
