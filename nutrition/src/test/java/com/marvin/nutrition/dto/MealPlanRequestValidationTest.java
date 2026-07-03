package com.marvin.nutrition.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests that validate Bean Validation constraints on the meal-plan write request records,
 * ensuring oversized/malformed input is rejected as a 400 instead of reaching the database and
 * surfacing as a generic 409 {@code DataIntegrityViolationException}.
 */
public class MealPlanRequestValidationTest {

    private static final String BADGE_OK = "ok";
    private static final String BADGE_WARN = "warn";

    private final Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("UpdateMealPlanRequest with all valid-length fields yields zero violations")
    void updateMealPlanRequest_valid_noViolations() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest(
                "eyebrow", "title", "description", "shoppingListTitle", "shoppingListNote",
                "shoppingListCallout", "footerNote"
        );

        final Set<ConstraintViolation<UpdateMealPlanRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("UpdateMealPlanRequest with over-long eyebrow yields a violation on 'eyebrow'")
    void updateMealPlanRequest_overLongEyebrow_yieldsViolation() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest(
                "x".repeat(501), null, null, null, null, null, null
        );

        final Set<ConstraintViolation<UpdateMealPlanRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for eyebrow exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("eyebrow")));
    }

    @Test
    @DisplayName("UpdateMealPlanRequest with over-long title yields a violation on 'title'")
    void updateMealPlanRequest_overLongTitle_yieldsViolation() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest(
                null, "x".repeat(501), null, null, null, null, null
        );

        final Set<ConstraintViolation<UpdateMealPlanRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for title exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("UpdateMealPlanRequest with over-long shoppingListTitle yields a violation")
    void updateMealPlanRequest_overLongShoppingListTitle_yieldsViolation() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest(
                null, null, null, "x".repeat(501), null, null, null
        );

        final Set<ConstraintViolation<UpdateMealPlanRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for shoppingListTitle exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("shoppingListTitle")));
    }

    @Test
    @DisplayName("UpdateMealPlanRequest with over-long shoppingListNote yields a violation")
    void updateMealPlanRequest_overLongShoppingListNote_yieldsViolation() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest(
                null, null, null, null, "x".repeat(501), null, null
        );

        final Set<ConstraintViolation<UpdateMealPlanRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for shoppingListNote exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("shoppingListNote")));
    }

    @Test
    @DisplayName("UpdateMealPlanRequest with unbounded TEXT fields at large size yields zero violations")
    void updateMealPlanRequest_largeUnboundedTextFields_noViolations() {
        final UpdateMealPlanRequest req = new UpdateMealPlanRequest(
                null, null, "x".repeat(5000), null, null, "x".repeat(5000), "x".repeat(5000)
        );

        final Set<ConstraintViolation<UpdateMealPlanRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for unbounded TEXT columns regardless of length");
    }

    @Test
    @DisplayName("UpdateMealPlanSectionRequest with valid-length fields yields zero violations")
    void updateMealPlanSectionRequest_valid_noViolations() {
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest("title", "note", "callout");

        final Set<ConstraintViolation<UpdateMealPlanSectionRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("UpdateMealPlanSectionRequest with over-long title yields a violation")
    void updateMealPlanSectionRequest_overLongTitle_yieldsViolation() {
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest("x".repeat(501), null, null);

        final Set<ConstraintViolation<UpdateMealPlanSectionRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for title exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("UpdateMealPlanSectionRequest with over-long note yields a violation")
    void updateMealPlanSectionRequest_overLongNote_yieldsViolation() {
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest(null, "x".repeat(501), null);

        final Set<ConstraintViolation<UpdateMealPlanSectionRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for note exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("note")));
    }

    @Test
    @DisplayName("UpdateMealPlanSectionRequest with large unbounded callout yields zero violations")
    void updateMealPlanSectionRequest_largeCallout_noViolations() {
        final UpdateMealPlanSectionRequest req = new UpdateMealPlanSectionRequest(null, null, "x".repeat(5000));

        final Set<ConstraintViolation<UpdateMealPlanSectionRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for unbounded TEXT column regardless of length");
    }

    @Test
    @DisplayName("UpdateMealPlanRowRequest with valid-length fields yields zero violations")
    void updateMealPlanRowRequest_valid_noViolations() {
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest("meal", "details", "qty", "kcal", "protein");

        final Set<ConstraintViolation<UpdateMealPlanRowRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("UpdateMealPlanRowRequest with over-long meal yields a violation")
    void updateMealPlanRowRequest_overLongMeal_yieldsViolation() {
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest("x".repeat(256), null, null, null, null);

        final Set<ConstraintViolation<UpdateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for meal exceeding 255 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("meal")));
    }

    @Test
    @DisplayName("UpdateMealPlanRowRequest with over-long qty yields a violation")
    void updateMealPlanRowRequest_overLongQty_yieldsViolation() {
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(null, null, "x".repeat(256), null, null);

        final Set<ConstraintViolation<UpdateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for qty exceeding 255 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("qty")));
    }

    @Test
    @DisplayName("UpdateMealPlanRowRequest with over-long kcal yields a violation")
    void updateMealPlanRowRequest_overLongKcal_yieldsViolation() {
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(null, null, null, "x".repeat(256), null);

        final Set<ConstraintViolation<UpdateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for kcal exceeding 255 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("kcal")));
    }

    @Test
    @DisplayName("UpdateMealPlanRowRequest with over-long protein yields a violation")
    void updateMealPlanRowRequest_overLongProtein_yieldsViolation() {
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(null, null, null, null, "x".repeat(256));

        final Set<ConstraintViolation<UpdateMealPlanRowRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for protein exceeding 255 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("protein")));
    }

    @Test
    @DisplayName("UpdateMealPlanRowRequest with large unbounded details yields zero violations")
    void updateMealPlanRowRequest_largeDetails_noViolations() {
        final UpdateMealPlanRowRequest req = new UpdateMealPlanRowRequest(null, "x".repeat(5000), null, null, null);

        final Set<ConstraintViolation<UpdateMealPlanRowRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for unbounded TEXT column regardless of length");
    }

    @Test
    @DisplayName("UpdateMealPlanStatRequest with valid-length fields yields zero violations")
    void updateMealPlanStatRequest_valid_noViolations() {
        final UpdateMealPlanStatRequest req = new UpdateMealPlanStatRequest("label", "value");

        final Set<ConstraintViolation<UpdateMealPlanStatRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("UpdateMealPlanStatRequest with over-long label yields a violation")
    void updateMealPlanStatRequest_overLongLabel_yieldsViolation() {
        final UpdateMealPlanStatRequest req = new UpdateMealPlanStatRequest("x".repeat(256), null);

        final Set<ConstraintViolation<UpdateMealPlanStatRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for label exceeding 255 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("label")));
    }

    @Test
    @DisplayName("UpdateMealPlanStatRequest with over-long value yields a violation")
    void updateMealPlanStatRequest_overLongValue_yieldsViolation() {
        final UpdateMealPlanStatRequest req = new UpdateMealPlanStatRequest(null, "x".repeat(256));

        final Set<ConstraintViolation<UpdateMealPlanStatRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for value exceeding 255 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("value")));
    }

    @Test
    @DisplayName("UpdateMealPlanShoppingCategoryRequest with valid-length title yields zero violations")
    void updateMealPlanShoppingCategoryRequest_valid_noViolations() {
        final UpdateMealPlanShoppingCategoryRequest req = new UpdateMealPlanShoppingCategoryRequest("title");

        final Set<ConstraintViolation<UpdateMealPlanShoppingCategoryRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("UpdateMealPlanShoppingCategoryRequest with over-long title yields a violation")
    void updateMealPlanShoppingCategoryRequest_overLongTitle_yieldsViolation() {
        final UpdateMealPlanShoppingCategoryRequest req = new UpdateMealPlanShoppingCategoryRequest("x".repeat(501));

        final Set<ConstraintViolation<UpdateMealPlanShoppingCategoryRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for title exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("UpdateMealPlanShoppingItemRequest with valid-length fields and null badge yields zero violations")
    void updateMealPlanShoppingItemRequest_validNullBadge_noViolations() {
        final UpdateMealPlanShoppingItemRequest req =
                new UpdateMealPlanShoppingItemRequest("name", "brand", null, "badgeText", "qty");

        final Set<ConstraintViolation<UpdateMealPlanShoppingItemRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations when badge is null");
    }

    @Test
    @DisplayName("UpdateMealPlanShoppingItemRequest with badge 'ok' yields zero violations")
    void updateMealPlanShoppingItemRequest_badgeOk_noViolations() {
        final UpdateMealPlanShoppingItemRequest req =
                new UpdateMealPlanShoppingItemRequest(null, null, BADGE_OK, null, null);

        final Set<ConstraintViolation<UpdateMealPlanShoppingItemRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for badge 'ok'");
    }

    @Test
    @DisplayName("UpdateMealPlanShoppingItemRequest with badge 'warn' yields zero violations")
    void updateMealPlanShoppingItemRequest_badgeWarn_noViolations() {
        final UpdateMealPlanShoppingItemRequest req =
                new UpdateMealPlanShoppingItemRequest(null, null, BADGE_WARN, null, null);

        final Set<ConstraintViolation<UpdateMealPlanShoppingItemRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for badge 'warn'");
    }

    @Test
    @DisplayName("UpdateMealPlanShoppingItemRequest with invalid badge value yields a violation on 'badge'")
    void updateMealPlanShoppingItemRequest_invalidBadge_yieldsViolation() {
        final UpdateMealPlanShoppingItemRequest req =
                new UpdateMealPlanShoppingItemRequest(null, null, "warning", null, null);

        final Set<ConstraintViolation<UpdateMealPlanShoppingItemRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for badge value not in {ok, warn}");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("badge")));
    }

    @Test
    @DisplayName("UpdateMealPlanShoppingItemRequest with blank badge yields a violation on 'badge'")
    void updateMealPlanShoppingItemRequest_blankBadge_yieldsViolation() {
        final UpdateMealPlanShoppingItemRequest req =
                new UpdateMealPlanShoppingItemRequest(null, null, "   ", null, null);

        final Set<ConstraintViolation<UpdateMealPlanShoppingItemRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for blank badge");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("badge")));
    }

    @Test
    @DisplayName("UpdateMealPlanShoppingItemRequest with over-long name yields a violation")
    void updateMealPlanShoppingItemRequest_overLongName_yieldsViolation() {
        final UpdateMealPlanShoppingItemRequest req =
                new UpdateMealPlanShoppingItemRequest("x".repeat(256), null, null, null, null);

        final Set<ConstraintViolation<UpdateMealPlanShoppingItemRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for name exceeding 255 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    @DisplayName("UpdateMealPlanShoppingItemRequest with over-long brand yields a violation")
    void updateMealPlanShoppingItemRequest_overLongBrand_yieldsViolation() {
        final UpdateMealPlanShoppingItemRequest req =
                new UpdateMealPlanShoppingItemRequest(null, "x".repeat(501), null, null, null);

        final Set<ConstraintViolation<UpdateMealPlanShoppingItemRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for brand exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("brand")));
    }

    @Test
    @DisplayName("UpdateMealPlanShoppingItemRequest with over-long badgeText yields a violation")
    void updateMealPlanShoppingItemRequest_overLongBadgeText_yieldsViolation() {
        final UpdateMealPlanShoppingItemRequest req =
                new UpdateMealPlanShoppingItemRequest(null, null, null, "x".repeat(501), null);

        final Set<ConstraintViolation<UpdateMealPlanShoppingItemRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for badgeText exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("badgeText")));
    }

    @Test
    @DisplayName("UpdateMealPlanShoppingItemRequest with over-long qty yields a violation")
    void updateMealPlanShoppingItemRequest_overLongQty_yieldsViolation() {
        final UpdateMealPlanShoppingItemRequest req =
                new UpdateMealPlanShoppingItemRequest(null, null, null, null, "x".repeat(256));

        final Set<ConstraintViolation<UpdateMealPlanShoppingItemRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for qty exceeding 255 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("qty")));
    }

    @Test
    @DisplayName("UpdateMealPlanSourceRequest with valid-length fields yields zero violations")
    void updateMealPlanSourceRequest_valid_noViolations() {
        final UpdateMealPlanSourceRequest req = new UpdateMealPlanSourceRequest("label", "https://example.com");

        final Set<ConstraintViolation<UpdateMealPlanSourceRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("UpdateMealPlanSourceRequest with over-long label yields a violation")
    void updateMealPlanSourceRequest_overLongLabel_yieldsViolation() {
        final UpdateMealPlanSourceRequest req = new UpdateMealPlanSourceRequest("x".repeat(501), null);

        final Set<ConstraintViolation<UpdateMealPlanSourceRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for label exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("label")));
    }

    @Test
    @DisplayName("UpdateMealPlanSourceRequest with over-long url yields a violation")
    void updateMealPlanSourceRequest_overLongUrl_yieldsViolation() {
        final UpdateMealPlanSourceRequest req = new UpdateMealPlanSourceRequest(null, "x".repeat(1001));

        final Set<ConstraintViolation<UpdateMealPlanSourceRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for url exceeding 1000 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("url")));
    }

    @Test
    @DisplayName("CreateMealPlanChangelogEntryRequest with valid-length fields yields zero violations")
    void createMealPlanChangelogEntryRequest_valid_noViolations() {
        final CreateMealPlanChangelogEntryRequest req =
                new CreateMealPlanChangelogEntryRequest("tag", "was", "text", 0);

        final Set<ConstraintViolation<CreateMealPlanChangelogEntryRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for a valid request");
    }

    @Test
    @DisplayName("CreateMealPlanChangelogEntryRequest with over-long tag yields a violation")
    void createMealPlanChangelogEntryRequest_overLongTag_yieldsViolation() {
        final CreateMealPlanChangelogEntryRequest req =
                new CreateMealPlanChangelogEntryRequest("x".repeat(256), null, "text", 0);

        final Set<ConstraintViolation<CreateMealPlanChangelogEntryRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for tag exceeding 255 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("tag")));
    }

    @Test
    @DisplayName("CreateMealPlanChangelogEntryRequest with over-long was yields a violation")
    void createMealPlanChangelogEntryRequest_overLongWas_yieldsViolation() {
        final CreateMealPlanChangelogEntryRequest req =
                new CreateMealPlanChangelogEntryRequest("tag", "x".repeat(501), "text", 0);

        final Set<ConstraintViolation<CreateMealPlanChangelogEntryRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty(), "Expected a violation for was exceeding 500 chars");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("was")));
    }

    @Test
    @DisplayName("CreateMealPlanChangelogEntryRequest with large unbounded text yields zero violations")
    void createMealPlanChangelogEntryRequest_largeText_noViolations() {
        final CreateMealPlanChangelogEntryRequest req =
                new CreateMealPlanChangelogEntryRequest("tag", null, "x".repeat(5000), 0);

        final Set<ConstraintViolation<CreateMealPlanChangelogEntryRequest>> violations = validator.validate(req);

        assertTrue(violations.isEmpty(), "Expected zero violations for unbounded TEXT column regardless of length");
    }
}
