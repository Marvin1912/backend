package com.marvin.nutrition.entity;

/** Indicates how a food entry was created or sourced. */
public enum FoodSource {
    /** Entry was created manually by the user. */
    MANUAL,
    /** Entry was parsed from a photo of a nutrition label. */
    PHOTO,
    /** Entry is an estimated approximation. */
    ESTIMATE,
    /** Entry was looked up via a barcode scan. */
    BARCODE
}
