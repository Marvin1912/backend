---
name: Post-refactor module structure
description: costs and backup modules replaced database/importer; which modules depend on what for JPA/Flyway
type: project
---

The `database` and `importer` modules were removed as part of GitHub issue #79 (2026-04-03).

- `costs` module: holds all cost-related logic (DailyCost, MonthlyCost, Salary, SpecialCost), repositories, importer services, Flyway for `finance` schema (`db/migration/costs`), infrastructure (Ibans), DirectoryWatcher, Delegator, GenericFileReader
- `backup` module: holds backup upload logic (BackupDirectoryWatcher, BackupUploadHandler, BackupTrackingService), BackupRunRepository, Flyway for `exports` schema (`db/migration/exports`)

**Why:** Both had JPA/Flyway/Hibernate-Envers declared as `api` deps — these transitively satisfy JpaRepository usage in feature modules (plants, image-server, it-news, mental-arithmetic).

**How to apply:** Feature modules that previously depended on `:database` now depend on `:costs` for JPA/Flyway transitive deps. The `vocabulary` module depends on `:backup` (uses BackupRunRepository for Anki sync tracking). The `api` module depends on both `:costs` and `:backup`.

The `consul` module (custom-built, NOT Spring Cloud Consul) was removed entirely as part of GitHub issue #194 (2026-06-18). It used to poll Consul every minute (`@Scheduled` in `CostConsulRepository`) for three IBAN filter lists consumed by `costs`. These are now plain `@Value`-injected env vars resolved once at startup: `SalaryImportIbansEnv`, `SpecialCostBlockedIbansEnv`, `MonthlyCostBlockedIbansEnv` (package `com.marvin.costs.infrastructure`), wired in `application.yaml` via `salary.import.ibans`, `special.cost.blocked-ibans`, `monthly.cost.blocked-ibans` -> `SALARY_IMPORT_IBANS`/`SPECIAL_COST_BLOCKED_IBANS`/`MONTHLY_COST_BLOCKED_IBANS` env vars (no defaults). The `costs` module gained its first `src/test` directory and standard JUnit5/Mockito/reactor-test dependencies as part of this change (it had none before).

Pre-existing checkstyle failures exist in: `camt`, `entities`, `image-server`, `vocabulary`, `mental-arithmetic`, `uploader` — these are NOT introduced by recent refactoring and predate issue #194. A pre-existing, unrelated flaky test also exists in `it-news` (`RssFetcherServiceTest.shouldSaveArticlePublishedWithinOneMonth`, date-boundary related) and `nutrition` requires Testcontainers/Docker (never run locally, see [[feedback_no_docker]] in the global memory).
