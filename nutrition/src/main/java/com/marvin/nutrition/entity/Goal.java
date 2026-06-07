package com.marvin.nutrition.entity;

/** Dietary goal that adjusts target calories relative to maintenance. */
public enum Goal {
    /** Caloric deficit for fat loss. */
    CUT,
    /** Calories at maintenance level. */
    MAINTAIN,
    /** Caloric surplus for muscle gain. */
    BULK
}
