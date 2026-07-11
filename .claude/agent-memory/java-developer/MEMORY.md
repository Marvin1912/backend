# Memory Index

- [Project module structure](project_module_structure.md) — costs and backup modules replaced database and importer; key dependency wiring for feature modules
- [New DTOs need DB-matching validation](feedback_dto_db_constraint_validation.md) — add @Size/@Pattern mirroring VARCHAR length/CHECK constraints when writing new request DTOs, don't rely on the DB to reject bad input
- [Record field addition test compat](feedback_record_field_addition_test_compat.md) — add a backward-compatible secondary constructor instead of touching existing tests' call sites when adding record components
- [Meal-plan write API](project_meal_plan_write_api.md) — nutrition module's meal-plan content write endpoints; rows are food-backed as of issue #225, stats/changelog/shopping-list removed
- [Checkstyle param-limit service split](feedback_checkstyle_param_limit_service_split.md) — split a write-service facade into delegate services instead of exceeding the 7-param constructor limit
- [Avoid unnecessary stubs when extending DI](feedback_avoid_unnecessary_stubs_when_extending_di.md) — check Mockito's default return value before adding stubs to an existing test for a new constructor dependency
- [sort_order: max not count](feedback_sort_order_max_not_count.md) — deriving a new row's position from count() of siblings collides once anything's been deleted from the middle; always use max(existing)+1
