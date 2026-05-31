package com.marvin.grocery.parser;

import java.time.LocalDate;
import java.util.List;

/**
 * Holds the structured result of parsing a raw OCR receipt text.
 *
 * @param items       the list of parsed line items found on the receipt
 * @param receiptDate the date extracted from the receipt, or null if not found
 */
public record ParsedReceipt(List<ParsedItem> items, LocalDate receiptDate) {
}
