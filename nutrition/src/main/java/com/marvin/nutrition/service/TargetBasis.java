package com.marvin.nutrition.service;

/** Indicates the source used to derive the basal metabolic rate. */
public enum TargetBasis {
    /** BMR was supplied directly as a manual override via {@code basal_kcal}. */
    BASAL_KCAL,
    /** BMR was calculated using the Mifflin–St Jeor formula. */
    MIFFLIN_ST_JEOR
}
