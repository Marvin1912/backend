---
name: feedback-snapshot-blocking-tx
description: Established pattern for @Transactional blocking JPA services invoked from reactive code on boundedElastic in the nutrition module
metadata:
  type: feedback
---

In the nutrition module, blocking JPA service methods are wrapped at the orchestration layer in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`. The blocking service methods themselves carry `@Transactional` (or `REQUIRES_NEW` for `ensureSnapshot`) and are invoked *across* Spring beans (e.g. `WeightService` -> `DayTargetSnapshotService`), so the transactional proxy is honored.

**Why:** This is the validated existing convention — do not flag a cross-bean `@Transactional` call from inside a `fromCallable` as a self-invocation/proxy bypass bug. Self-invocation only breaks `@Transactional` when a bean calls its *own* annotated method directly (e.g. a public `@Transactional` method calling another `@Transactional` method on `this`). Private helpers like `refreshSnapshot` correctly run inside the caller's transaction with no annotation.

**How to apply:** Only raise a transactional-proxy concern when an annotated method is reached via `this.` self-invocation, or when a `@Transactional` method is called from a thread with no Spring-managed proxy in the path. Cross-bean calls and private intra-transaction helpers are fine.
