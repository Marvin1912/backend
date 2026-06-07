---
name: openfoodfacts-api
description: OpenFoodFacts v2 product API not-found semantics — returns HTTP 200 with status:0, not 404
metadata:
  type: reference
---

The `BarcodeLookup` service (nutrition module) queries OpenFoodFacts v2: `GET /api/v2/product/{ean}.json`.

**Key non-obvious behavior (verified live 2026-06-07):** for an unknown/invalid barcode the API returns **HTTP 200** with body `{"code":..., "status":0, "status_verbose":"..."}` and **no `product` object** — it does NOT return HTTP 404.

**How to apply:** When reviewing `BarcodeLookup` not-found handling, the real not-found signal is the absent `product` field in `parseResponse` (emits `NoSuchElementException`), not the `HttpStatus.NOT_FOUND` branch in `onErrorMap` — that 404 branch is defensive and rarely fires. Tests should (and do) cover the absent-product path, not just a mocked 404. Nutriment fields (`*_100g`) are user-contributed and may be missing or string-typed; code guards by requiring name + kcal else 422. See [[nutrition-claude-pattern]] for the shared transient-DTO conventions.
