# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build the entire project
./gradlew build

# Build a specific module
./gradlew :plants:build

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :plants:test

# Run a single test class
./gradlew :plants:test --tests PlantControllerTest

# Run a single test method
./gradlew :plants:test --tests PlantControllerTest.testMethod

# Checkstyle (enforced, zero warnings allowed)
./gradlew checkstyleMain checkstyleTest
./gradlew checkstyleAll    # all subprojects at once

# Build the Spring Boot JAR (boot module only)
./gradlew :boot:bootJar

# Build Docker image
./gradlew buildAdapterDockerImage

# Local PostgreSQL (costs-db on port 5432, user=marvin, password=password, db=costs)
docker compose -f docker-compose-local.yaml up -d
```

## Architecture

This is a **multi-module Gradle monolith** using Spring Boot with an orthogonal architecture. All modules are packaged into a single Spring Boot JAR (`backend.jar`) via the `boot` module. Every module except `common` automatically depends on `common` (configured in root `build.gradle`).

### Core Modules

- **boot** - Application entry point (`com.marvin.Application`). Aggregates all 16 modules, enables scheduling. Only module that produces a bootJar.
- **common** - Shared utilities (JacksonMapper, DTOs, NullSafeUtil). No dependencies of its own.
- **entities** - Orphaned. Directory has no sources and is not in `settings.gradle`. JPA entities now live within their owning modules; the shared `BasicEntity` (providing `creationDate`/`lastModified`) lives in `costs` (`com.marvin.costs.entity.BasicEntity`).
- **api** - REST API facade and orchestration layer. Depends on costs, backup, exporter, uploader, camt.

### Domain Modules

- **costs** - Financial cost management (daily/special costs, accounting imports). Owns Flyway migrations, depends on entities, influxdb, consul, camt.
- **backup** - Data backup operations tracking and run history. Uses Hibernate Envers for audit trail. Depends on entities, uploader.
- **camt** - CAMT (ISO 20022) XML bank message parsing. Uses xjc plugin for schema-to-Java generation.

### Infrastructure Modules

- **influxdb** - InfluxDB time-series client wrapper for metrics storage.
- **consul** - HashiCorp Consul KV client for distributed config/secrets.
- **exporter** - Pass-through module re-exporting costs, influxdb, vocabulary dependencies.
- **uploader** - Google Drive file upload/download and ZIP compression.

### Feature Modules

Self-contained vertical slices, each with own Flyway migrations in separate schemas:

- **plants** - Plant/gardening tracking with images and cost associations. Includes Prometheus metrics.
- **it-news** - IT news RSS feed aggregator with feed config management.
- **vocabulary** - Language learning with Anki sync integration. Uses OpenAPI generator.
- **mental-arithmetic** - Arithmetic exercises with difficulty levels and performance tracking.
- **image-server** - Image upload/storage and retrieval service.
- **climate** - Temperature/climate readings. Depends on influxdb.
- **grocery** - Receipt/grocery tracking with Tesseract OCR (`tess4j`) and receipt parsing. Depends on costs; owns Flyway migrations.

### Legacy (not in settings.gradle)

The `database/` and `importer/` directories exist but are not included as Gradle modules. They are orphaned and not wired into the boot application.

### Key Technical Choices

- **Reactive stack**: Spring WebFlux (not MVC) — controllers return `Mono<>` / `Flux<>`
- **Mapping**: MapStruct for entity-DTO conversion (`@Mapper(componentModel = "spring")`)
- **Metrics**: Micrometer/Prometheus gauges (e.g., plant watering/fertilizing status)
- **API docs**: Springdoc OpenAPI with WebFlux UI
- **`-parameters` compiler flag** is enabled (preserves parameter names at runtime)

## Testing

- JUnit 5 + Mockito + `reactor-test` (StepVerifier for reactive assertions)
- Tests use `@ExtendWith(MockitoExtension.class)` for unit tests

### Test-Driven Development (mandatory)

- **Write the test first.** For every piece of logic, write a failing test before writing the
  implementation. Follow the red → green → refactor cycle.
- **Tests are the contract — do not change them to make code pass.** Once a test is written, the
  implementation (logic) is what must change. The logic has the highest priority and must be
  adapted to satisfy the tests, never the other way around.
- **If a test genuinely needs to change, ask the user first** before modifying it.

## Agent Workflow

- **All development tasks** must use the `java-developer` agent.
- **After development is complete** and a PR has been created, use the `java-code-reviewer` agent to review the changes before merging.
- **When a GitHub issue has been finished**, a PR must be created and merged into `master`.
- **For every new feature**, use the `tdd-test-guardian` agent to verify that existing tests were
  not modified. If tests were changed, it must stop and ask the user before anything proceeds.
