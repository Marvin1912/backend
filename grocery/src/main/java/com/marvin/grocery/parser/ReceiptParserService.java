package com.marvin.grocery.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Parses raw OCR text from German grocery receipts into structured item lists. */
@Service
public class ReceiptParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptParserService.class);

    private static final Pattern ITEM_PATTERN =
            Pattern.compile("^(.+?)\\s{2,}(-?\\d{1,4}[.,]\\d{2})\\s{2,}(\\d{1,4})\\s{2,}(-?\\d{1,4}[.,]\\d{2})\\s*$");

    private static final Pattern TOTAL_LINE_PATTERN =
            Pattern.compile("(?i)(summe|gesamt|total|zwischensumme|zu zahlen|bar|ec.karte|mwst|ust)");

    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Parses raw OCR text into a structured receipt with items and optional date.
     *
     * @param rawText the raw OCR text from a receipt image
     * @return a ParsedReceipt containing detected items and receipt date
     */
    public ParsedReceipt parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new ParsedReceipt(List.of(), null);
        }

        final String[] lines = rawText.split("\\r?\\n");
        final List<ParsedItem> items = new ArrayList<>();
        LocalDate receiptDate = null;

        for (final String line : lines) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (receiptDate == null) {
                receiptDate = tryExtractDate(trimmed);
            }

            final ParsedItem item = tryParseItem(trimmed);
            if (item != null) {
                items.add(item);
            }
        }

        return new ParsedReceipt(List.copyOf(items), receiptDate);
    }

    /**
     * Attempts to extract a date in DD.MM.YYYY format from a line of text.
     *
     * @param line the text line to search
     * @return the parsed date or null if none found
     */
    private LocalDate tryExtractDate(String line) {
        final Matcher dateMatcher = DATE_PATTERN.matcher(line);
        if (!dateMatcher.find()) {
            return null;
        }
        try {
            return LocalDate.parse(dateMatcher.group(1), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            LOGGER.debug("Could not parse date from: {}", dateMatcher.group(1));
            return null;
        }
    }

    /**
     * Attempts to parse a single receipt line into a ParsedItem.
     *
     * @param line the text line to parse
     * @return a ParsedItem if the line matches the four-column item pattern, otherwise null
     */
    private ParsedItem tryParseItem(String line) {
        if (TOTAL_LINE_PATTERN.matcher(line).find()) {
            return null;
        }

        final Matcher matcher = ITEM_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }

        final String name = matcher.group(1).trim();
        final String singlePriceRaw = matcher.group(2).replace(',', '.');
        final String quantityRaw = matcher.group(3);
        final String totalPriceRaw = matcher.group(4).replace(',', '.');

        try {
            final BigDecimal singlePrice = new BigDecimal(singlePriceRaw);
            final int quantity = Integer.parseInt(quantityRaw);
            final BigDecimal totalPrice = new BigDecimal(totalPriceRaw);
            return new ParsedItem(name, singlePrice, quantity, totalPrice);
        } catch (NumberFormatException e) {
            LOGGER.debug("Could not parse item fields from line: {}", line);
            return null;
        }
    }
}
