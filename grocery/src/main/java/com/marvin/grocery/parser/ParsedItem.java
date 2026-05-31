package com.marvin.grocery.parser;

import java.math.BigDecimal;

/**
 * Represents a single parsed item from a raw OCR receipt text.
 *
 * @param name        name of the item as read from the receipt
 * @param singlePrice price per unit of the item
 * @param quantity    number of units purchased
 * @param price       total line price (singlePrice × quantity)
 */
public record ParsedItem(String name, BigDecimal singlePrice, int quantity, BigDecimal price) {
}
