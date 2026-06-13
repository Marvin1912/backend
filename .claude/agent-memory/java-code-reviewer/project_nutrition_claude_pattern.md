---
name: nutrition-claude-pattern
description: Established structure for Claude-API-backed services in the nutrition module (LabelReader, MealEstimator)
metadata:
  type: project
---

The nutrition module has an established pattern for Claude-API-backed services. `LabelReader` (image/vision) was the first; `MealEstimator` (text-only) mirrors it.

**Why:** Consistency across Claude integrations keeps error handling, API-key handling, and JSON parsing uniform and reviewable.

**How to apply:** When reviewing a new Claude-backed service, check it matches the LabelReader template:
- WebClient built in constructor with `baseUrl https://api.anthropic.com`, headers `x-api-key` (from `@Value("${nutrition.claude.api-key}")`) and `anthropic-version 2023-06-01`. The api-key property is resolved externally (env `NUTRITION_CLAUDE_API_KEY` / consul) and is NOT in any application.yml — that is intentional, not a hardcoded-secret finding.
- model `claude-sonnet-4-6`, max_tokens 1024.
- `.bodyToMono(Map.class)` then `.onErrorMap(WebClientResponseException.class, ...)` to a domain exception, then `flatMap(parseClaudeResponse)`.
- `parseClaudeResponse` pulls `content[0].text`, parses strict JSON into a `@JsonIgnoreProperties(ignoreUnknown=true)` record DTO, emits the domain exception on parse failure or missing key field.
- Domain exception (e.g. MealEstimateException) extends RuntimeException, mapped to HTTP 422 in `NutritionExceptionHandler`.
- All nutrition controllers share the same `@Tag(name="Nutrition", description="Nutrition profile, weight tracking and target calculation")`.
- Transient DTOs (FoodDraftDTO, MealEstimateDTO) are never persisted; field names of MealEstimateDTO deliberately match `CreateMealEntryRequest` ad-hoc macro fields (kcal/proteinG/carbsG/fatG) so the estimate is a drop-in meal_entry payload.

`NutritionExceptionHandler` historically had no unit tests. As of the foods-pagination PR (2026-06, branch feature/nutrition-foods-pagination) a `NutritionExceptionHandlerTest` exists but covers only the new `handleConstraintViolation` 400 mapping. The 422 mappings (LabelReadException, MealEstimateException, BarcodeLookupException) and the 404/409/400 mappings remain untested — so a missing handler test for those is still a pre-existing gap, not a per-PR regression.
