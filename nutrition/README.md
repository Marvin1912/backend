# Nutrition Module

Self-contained vertical slice for nutrition tracking: food catalog, daily meal logging, weight tracking, and target calculation.

## Design assumptions

### Single-user / single-tenant

This module is intentionally built for a **single user**. There is no authentication, no `userId`, and no tenancy column anywhere. These assumptions are baked into the schema:

- **The profile is a hard singleton.** `ProfileEntity.SINGLETON_ID = 1`; the row's `id` is fixed at `1` and a DB-level `CHECK` constraint enforces that no other id can exist. There is exactly one nutrition profile.
- **Weight entries are globally unique per date.** `weight_entry.entry_date` carries a global `UNIQUE` constraint (one weight per calendar day for the whole table, not per user).
- **No user scoping.** Foods, meal entries, weight entries and targets are all global.

> ⚠️ **Adding multi-tenancy later is a schema-breaking change.** Supporting more than one user would require dropping the profile singleton `CHECK`, relaxing the `entry_date` uniqueness to `(user_id, entry_date)`, introducing a `user_id` foreign key on every table, and adding authentication. Treat the single-user assumption as a deliberate constraint, not an oversight.

### Transaction boundaries

Every write runs in its **own implicit transaction**. The services follow the reactive pattern

```java
Mono.fromCallable(() -> { /* blocking JPA work */ })
    .subscribeOn(Schedulers.boundedElastic());
```

so each repository `save`/`delete` is auto-committed independently. There are no `@Transactional` boundaries because every current write touches a **single entity**, for which an implicit transaction is sufficient.

> ⚠️ **No cross-entity atomicity today.** If an operation is ever introduced that mutates more than one entity (e.g. saving a meal entry *and* updating an aggregate, or any multi-row write that must succeed or fail together), wrap the blocking work in a method annotated with `@Transactional` so the whole unit commits or rolls back atomically. Do **not** rely on the current per-call auto-commit behaviour for multi-entity invariants.

## Configuration

| Property | Environment variable | Description |
|---|---|---|
| `nutrition.claude.api-key` | `NUTRITION_CLAUDE_API_KEY` | Anthropic API key used by `LabelReader` to call Claude Vision |

Example (application.yml / environment):

```yaml
nutrition:
  claude:
    api-key: ${NUTRITION_CLAUDE_API_KEY}
```

## Nutrition Label Scanner

`POST /nutrition/foods/scan-label` (multipart/form-data, field name `file`)

Sends the uploaded JPEG or PNG image to the Claude Vision API, which reads the nutrition label and returns a **transient draft food** as JSON.

- The draft is **never persisted** — no database row is written and no image is stored.
- All macronutrient values are normalised to per 100 g (Claude converts from per-serving if needed).
- Nullable fields (`brand`, `fiberPer100`, `servingG`) are `null` when not present on the label.
- If Claude returns a response that cannot be parsed as JSON, the endpoint responds with **HTTP 422 Unprocessable Entity**.

### Known limitations

- **Image format support**: Only JPEG and PNG are recognised by the media-type sniff. HEIC and WebP images fall back to `image/jpeg` and may be rejected by the Claude Vision API upstream.
- **Upload size limit**: The WebFlux default in-memory codec limit (256 KB) applies to the uploaded label image, the same constraint that affects the grocery receipt upload. Images larger than 256 KB are rejected before reaching the service.
