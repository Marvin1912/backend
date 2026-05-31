package com.marvin.grocery.parser;

import java.math.BigDecimal;

/**
 * Represents a single parsed item from a raw OCR receipt text.
 *
 * @param name  name of the item as read from the receipt
 * @param price price of the item
 */
public record ParsedItem(String name, BigDecimal price) {
}
