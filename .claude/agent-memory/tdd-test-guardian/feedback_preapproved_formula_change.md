---
name: preapproved-formula-change
description: Pattern for verifying a feature that intentionally changes existing test assertions because a formula/constant the tests hardcoded was replaced (issue #204, CUT deficit flat -500 -> bodyweight-scaled).
metadata:
  type: feedback
---

When the user has pre-approved modifying specific existing tests because a feature intentionally
replaces a hardcoded constant/formula those tests encoded (verified on
`feature/204-cut-deficit-bodyweight-rate`, issue #204: `TargetService` CUT goal flat -500 kcal
deficit replaced with a bodyweight-rate-scaled formula per Garthe et al. 2011):

- The user named the exact test(s) they pre-approved changing up front in the request. Only those
  named tests (plus any other test that demonstrably hardcodes the same now-obsolete constant and
  would otherwise fail to compile/pass) should be modified — flag if scope crept beyond that.
- Verification recipe: diff the test file against HEAD, confirm (1) brand-new tests were added for
  the new behavior across a meaningful input range (here: 50/70/90 kg), (2) only the named existing
  test(s) changed, (3) every other existing test (different goals, edge cases, error paths) is
  byte-for-byte untouched — compare `grep -n "@Test\|void compute"` (or similar) output before/after
  to catch any other renamed/altered test quickly.
- Arithmetic check: recompute the new expected values independently (Python `round()` is safe to
  use for sanity-checking `Math.round` in Java as long as no exact `.5` boundary is hit — verify by
  also running the real `Math.round` logic, e.g. via a small scratch `.java` file compiled and run
  with `javac`/`java`, since floating-point representation can differ subtly from naive Python math).
- Always force a real test run (`--rerun` or after touching a source file) rather than trusting a
  cached `UP-TO-DATE` Gradle result when confirming "tests pass."

**Why:** Distinguishes a legitimate, narrowly-scoped, user-authorized test change (allowed) from an
agent unilaterally weakening tests to make code pass (blocked). The scope check (only named tests +
necessary same-root-cause tests) and the independent arithmetic re-derivation are what make the
verification actually meaningful rather than rubber-stamping.

**How to apply:** Use this same recipe for future "formula/constant swap" PRs where the user
explicitly says "I approved changing test X for reason Y" — check the diff stat /method-name list
for unexpected churn, recompute new expected values from the stated formula by hand and via a real
run of the production rounding semantics, and run the actual test suite (forced, not cached) before
concluding `TESTS UNCHANGED (ok)`-equivalent verdict for the narrow approved scope.

Related: [[consul-removal-clean-pattern]] for the analogous "removal/replacement" PR verification
recipe, a different shape of legitimate large-scale test churn (whole-module swap rather than a
single-formula change).

**Second confirmed instance (2026-07-03, branch `feature/nutrition-meal-plan-db-backed`):** same
recipe applies beyond pure formula swaps — a full rewrite of `MealPlanServiceTest` was pre-approved
in a written plan file (`change-the-current-code-delegated-forest.md`) up front because the old test
asserted classpath-loading + "same cached instance forever" behavior that no longer exists once
`MealPlanService` reads through repositories per call instead of caching a parsed JSON file. Verified
by diffing the test against `master`, confirming it was the *only* existing test touched
repo-wide (`git diff --name-only master | grep -E 'Test\.java$|Tests\.java$'`), and that the two new
sibling test files (`MealPlanRepositoryTest`, `MealPlanMapperTest`) were untracked additions, not
modifications. One gap worth flagging in this kind of assembler/repository extraction: newly
extracted helper classes (here `MealPlanSectionAssembler` / `MealPlanShoppingListAssembler`) can end
up with no direct unit test of their own — they're mocked away in the service test and only touched
incidentally by a `@DataJpaTest` repository test that exercises the underlying repositories, not the
assembler's orchestration logic. Flag this as an advisory (not a blocking violation) when it recurs.
