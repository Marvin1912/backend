# Memory Index

- [Nutrition Claude integration pattern](project_nutrition_claude_pattern.md) — how Claude-backed services (LabelReader, MealEstimator) are structured in the nutrition module
- [No Bash tool in review sessions](feedback_no_bash_build.md) — cannot run ./gradlew; reviews are static-analysis only unless a Bash tool is present
- [OpenFoodFacts v2 API](reference_openfoodfacts_api.md) — unknown barcodes return HTTP 200 + status:0 (not 404); affects BarcodeLookup not-found review
- [Nutrition profile singleton](project_nutrition_profile_singleton.md) — profile is DB-enforced single row pinned to id=1 (SINGLETON_ID); save() merges via assigned id
- [Day target snapshot](project_day_target_snapshot.md) — per-day target snapshot (V1_7); assigned-@Id merge like the singleton; check-then-act upsert, getDay no-snapshot fallback uses live targets
- [Weight entry constraint](project_weight_entry_constraint.md) — weight_entry unique date constraint is unnamed; 409 handler matches Postgres auto-name weight_entry_entry_date_key by prefix
