---
name: weight-entry-constraint
description: weight_entry uniqueness uses an unnamed inline UNIQUE; the 409 exception handler matches the Postgres auto-generated constraint name by prefix
metadata:
  type: project
---

The `nutrition.weight_entry.entry_date` unique constraint is declared inline as `DATE NOT NULL UNIQUE` in `V1_2__nutrition_profile_weight.sql` and via `@Column(unique = true)` on `WeightEntryEntity` — it is **not** explicitly named. Postgres auto-generates the name `weight_entry_entry_date_key` (pattern `{table}_{column}_key`).

`NutritionExceptionHandler.handleDataIntegrityViolation` distinguishes the weight-duplicate 409 from a generic 409 by inspecting the Hibernate `ConstraintViolationException` cause and checking `getConstraintName().startsWith("weight_entry")`. `WeightService.create` does a bare `save()` with no pre-check, so this DB constraint is the real (and only) source of the duplicate-date violation.

**Why:** The production match relies on an undocumented Postgres naming convention, not on anything pinned in the schema. Renaming the table, switching to a named/composite constraint, or Hibernate generating the DDL (UK-hash name) would silently break the match and downgrade users to the generic message. Unit tests cannot catch this because they construct the Hibernate exception with a literal name.

**How to apply:** When reviewing changes to this constraint, the weight entity/table, or the exception handler, flag the implicit coupling and recommend pinning the constraint name explicitly (mirrors the named `profile_single_row` check in `V1_6`). See [[project_nutrition_profile_singleton]].
