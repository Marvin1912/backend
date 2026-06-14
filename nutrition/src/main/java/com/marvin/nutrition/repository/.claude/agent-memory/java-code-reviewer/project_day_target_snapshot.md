---
name: project-day-target-snapshot
description: How nutrition day-target snapshots are computed/refreshed and the staleness invariant they must uphold
metadata:
  type: project
---

Day-target snapshots (`DayTargetSnapshotEntity`, keyed by `entryDate`) freeze the computed daily nutrition targets (bmr, maintenance/target kcal, macros) for a day, created lazily on first meal log via `DayTargetSnapshotService.ensureSnapshot`.

The applicable weight for any day is the most recent weight entry on or before that date (`findTopByEntryDateLessThanEqualOrderByEntryDateDesc`, with latest-known weight as fallback). So a single weight change affects a *range* of subsequent days `[changedDate, nextWeightEntryDate)`, not just the changed date. Profile edits affect *all* snapshots.

**Why:** Issue #165 — snapshots went stale on weight backfill/delete and profile edits, because the old refresh only touched the exact changed date. The tracker's core `remaining = target - consumed` depends on accurate targets.

**How to apply:** When reviewing snapshot-refresh logic, verify the affected range is correctly bounded (exclusive upper bound = next weight entry date), that refresh only touches *existing* snapshots (never creates new ones — that's `ensureSnapshot`'s job), and that profile changes trigger `refreshAllSnapshots`. Refreshes run after the triggering write is saved, so they see committed/current state. See [[feedback-snapshot-blocking-tx]].
