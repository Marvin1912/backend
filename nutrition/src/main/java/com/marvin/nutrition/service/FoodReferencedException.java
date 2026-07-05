package com.marvin.nutrition.service;

/**
 * Thrown when a food catalog entry cannot be deleted because it is still referenced by one or more
 * meal-plan rows or meal-template items.
 */
public class FoodReferencedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long mealPlanRowCount;
    private final long mealTemplateItemCount;

    /**
     * Creates a new FoodReferencedException carrying the reference counts that prevented deletion.
     *
     * @param mealPlanRowCount      number of meal-plan rows referencing the food
     * @param mealTemplateItemCount number of meal-template items referencing the food
     */
    public FoodReferencedException(long mealPlanRowCount, long mealTemplateItemCount) {
        super("Food is still referenced by " + mealPlanRowCount + " meal-plan row(s) and "
                + mealTemplateItemCount + " meal-template item(s)");
        this.mealPlanRowCount = mealPlanRowCount;
        this.mealTemplateItemCount = mealTemplateItemCount;
    }

    /**
     * Returns the number of meal-plan rows referencing the food.
     *
     * @return the meal-plan row reference count
     */
    public long getMealPlanRowCount() {
        return mealPlanRowCount;
    }

    /**
     * Returns the number of meal-template items referencing the food.
     *
     * @return the meal-template item reference count
     */
    public long getMealTemplateItemCount() {
        return mealTemplateItemCount;
    }
}
