package com.marvin.nutrition.entity;

/** Physical activity level used to derive maintenance calories from BMR. */
public enum ActivityLevel {
    /** Little or no exercise. */
    SEDENTARY,
    /** Light exercise 1–3 days per week. */
    LIGHT,
    /** Moderate exercise 3–5 days per week. */
    MODERATE,
    /** Hard exercise 6–7 days per week. */
    ACTIVE,
    /** Very hard exercise or a physically demanding job. */
    VERY_ACTIVE
}
