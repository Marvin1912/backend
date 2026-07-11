---
name: consul-removal-clean-pattern
description: Example of a clean TDD-compliant module removal (issue #194, consul -> env vars) used as a reference pattern for future "replace infra X with Y" PRs.
metadata:
  type: feedback
---

When a feature branch deletes a whole module/class and replaces it with a new implementation,
the TDD-compliant pattern looks like this (verified clean on `feature/194-remove-consul-integration`,
PR #196, issue #194):

- Old implementation classes (e.g. `SalaryImportIbansConsul`, `CostConsulRepository`, the whole
  `consul` Gradle module) are deleted outright.
- No test files existed for the deleted classes on master (checked via
  `git ls-tree -r master --name-only | grep -i <module> | grep -i test`), so there was nothing to
  delete on the test side — this is fine, not a violation.
- New implementation classes (`*IbansEnv`) come with brand-new test files
  (`*EnvTest.java`), added (not modified), each with full coverage of the parsing/edge-case logic
  (comma-split, trim, blank/empty/null handling, interface contract).
- Only non-test files changed besides the new logic: `build.gradle` (root, per-module),
  `settings.gradle`, `application.yaml` — pure wiring/config changes, not logic.

**Why:** Confirms the expected shape of a compliant removal-and-replace PR so future similar
reviews (infra swap-outs) can be checked quickly: verify (1) no surviving-logic test was edited,
(2) deleted classes had no orphaned tests left behind, (3) new classes have new tests with real
assertions, not stubs.

**How to apply:** For future "remove module X, replace with Y" PRs, use this same verification
recipe: `git diff --name-status master <branch> -- '**/src/test/**'` should show only additions
(or deletions matching deleted source classes), and spot-check at least one new test file against
its implementation to confirm assertions actually match the real logic.
