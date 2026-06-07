# Nutrition Module

Self-contained vertical slice for nutrition tracking: food catalog, daily meal logging, weight tracking, and target calculation.

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
