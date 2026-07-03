package com.marvin.nutrition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvin.nutrition.dto.MealPlanDTO;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Serves the static weekly meal-plan reference document bundled as a classpath resource.
 * The document is not user-editable data, so it is parsed once and cached in memory rather than
 * being persisted in the database.
 */
@Service
public class MealPlanService {

    private static final String MEAL_PLAN_RESOURCE = "nutrition/meal-plan.json";

    private final MealPlanDTO mealPlan;

    /**
     * Creates a new MealPlanService, eagerly loading and parsing the bundled meal-plan resource.
     *
     * @param objectMapper Jackson mapper used to parse the bundled JSON resource
     */
    public MealPlanService(ObjectMapper objectMapper) {
        this.mealPlan = readMealPlan(objectMapper);
    }

    /**
     * Returns the weekly meal-plan reference document.
     *
     * @return a Mono emitting the cached meal-plan DTO
     */
    public Mono<MealPlanDTO> getMealPlan() {
        return Mono.just(mealPlan);
    }

    private MealPlanDTO readMealPlan(ObjectMapper objectMapper) {
        try (InputStream inputStream = new ClassPathResource(MEAL_PLAN_RESOURCE).getInputStream()) {
            return objectMapper.readValue(inputStream, MealPlanDTO.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load bundled meal-plan resource: " + MEAL_PLAN_RESOURCE, e);
        }
    }
}
