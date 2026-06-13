---
name: nutrition-profile-singleton
description: The nutrition profile is a DB-enforced single-row table pinned to id=1 (SINGLETON_ID)
metadata:
  type: project
---

`nutrition.profile` is intentionally a single-row table. Issue #129 / PR #135 enforced this: migration V1_6 collapses duplicates, pins the surviving row to id=1, drops the BIGSERIAL default, and adds `CHECK (id = 1)` (constraint `profile_single_row`). `ProfileEntity` dropped `@GeneratedValue`, exposes `public static final long SINGLETON_ID = 1L`, and both `NutritionProfileService` and `NutritionTargetService` read via `findById(SINGLETON_ID)`.

**Why:** Deterministic single-profile reads without `findAll().get(0)` guesswork, and a hard DB guard against a second row from concurrent inserts.

**How to apply:** When reviewing nutrition profile code, expect id to always be 1 and assigned manually in `upsertProfile`. Because `BasicEntity` has no `@Version` and the entity does not implement `Persistable`, the assigned non-null id makes Spring Data's `isNew()` false, so `save()` always routes through Hibernate `merge()` (SELECT-then-INSERT/UPDATE) — this is correct for the singleton, not a bug. The CHECK(id=1) does NOT block a true concurrency race on first creation (two concurrent inserts both target id=1) — the primary-key uniqueness on id=1 is what serializes that; one wins, the other gets a duplicate-key error. See [[nutrition-claude-pattern]] for the broader module layout.
