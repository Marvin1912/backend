package com.marvin.grocery.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReceiptParserService Tests")
class ReceiptParserServiceTest {

    private ReceiptParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new ReceiptParserService();
    }

    @Test
    @DisplayName("Should parse a standard four-column receipt line")
    void parse_StandardFourColumnLine_ReturnsItem() {
        final String text = "Vollmilch 3,5%  1.09  1  1.09";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(1, result.items().size());
        final ParsedItem item = result.items().get(0);
        assertEquals("Vollmilch 3,5%", item.name());
        assertEquals(new BigDecimal("1.09"), item.singlePrice());
        assertEquals(1, item.quantity());
        assertEquals(new BigDecimal("1.09"), item.price());
    }

    @Test
    @DisplayName("Should parse an item with a multi-word name")
    void parse_MultiWordName_ReturnsItem() {
        final String text = "Bio Hafer Flocken  2.49  1  2.49";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(1, result.items().size());
        final ParsedItem item = result.items().get(0);
        assertEquals("Bio Hafer Flocken", item.name());
        assertEquals(new BigDecimal("2.49"), item.singlePrice());
        assertEquals(1, item.quantity());
        assertEquals(new BigDecimal("2.49"), item.price());
    }

    @Test
    @DisplayName("Should parse a line with quantity greater than one")
    void parse_MultipleQuantity_ReturnsItem() {
        final String text = "Butter  1.79  3  5.37";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(1, result.items().size());
        final ParsedItem item = result.items().get(0);
        assertEquals("Butter", item.name());
        assertEquals(new BigDecimal("1.79"), item.singlePrice());
        assertEquals(3, item.quantity());
        assertEquals(new BigDecimal("5.37"), item.price());
    }

    @Test
    @DisplayName("Should exclude total lines from items")
    void parse_TotalLine_IsExcluded() {
        final String text = "Apfel  0.89  1  0.89\nGesamt  0,89\nSumme  0,89";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(1, result.items().size());
        assertEquals("Apfel", result.items().get(0).name());
    }

    @Test
    @DisplayName("Should extract date in DD.MM.YYYY format")
    void parse_DateLine_ExtractsDate() {
        final String text = "Datum: 15.03.2024\nMilch  0.99  1  0.99";

        final ParsedReceipt result = parserService.parse(text);

        assertNotNull(result.receiptDate());
        assertEquals(LocalDate.of(2024, 3, 15), result.receiptDate());
    }

    @Test
    @DisplayName("Should return empty result for empty input")
    void parse_EmptyInput_ReturnsEmptyReceipt() {
        final ParsedReceipt result = parserService.parse("");

        assertTrue(result.items().isEmpty());
        assertNull(result.receiptDate());
    }

    @Test
    @DisplayName("Should return empty result for null input")
    void parse_NullInput_ReturnsEmptyReceipt() {
        final ParsedReceipt result = parserService.parse(null);

        assertTrue(result.items().isEmpty());
        assertNull(result.receiptDate());
    }

    @Test
    @DisplayName("Should parse multiple items from a multi-line receipt")
    void parse_MultipleItems_ReturnsAllItems() {
        final String text = "15.03.2024\nVollmilch  1.09  1  1.09\nButter  1.79  1  1.79\nBrot  2.49  1  2.49";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(3, result.items().size());
        assertEquals(LocalDate.of(2024, 3, 15), result.receiptDate());
    }

    @Test
    @DisplayName("Should exclude 'zu zahlen' total line")
    void parse_ZuZahlenLine_IsExcluded() {
        final String text = "Joghurt  0.79  1  0.79\nZu zahlen  0,79";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(1, result.items().size());
        assertEquals("Joghurt", result.items().get(0).name());
    }

    @Test
    @DisplayName("Should parse Claude Vision OCR sample with negative prices, quantities, and date")
    void parse_ClaudeVisionSample_ReturnsAllItemsAndDate() {
        final String text = "Cherrystrauchtomaten  2.99  2  5.98\n"
                + "Preisvorteil  -2.00  1  -2.00\n"
                + "Apfel pink  2.09  1  2.09\n"
                + "29.05.2026";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(3, result.items().size());
        assertEquals(LocalDate.of(2026, 5, 29), result.receiptDate());

        final ParsedItem tomaten = result.items().get(0);
        assertEquals("Cherrystrauchtomaten", tomaten.name());
        assertEquals(new BigDecimal("2.99"), tomaten.singlePrice());
        assertEquals(2, tomaten.quantity());
        assertEquals(new BigDecimal("5.98"), tomaten.price());

        final ParsedItem preisvorteil = result.items().get(1);
        assertEquals("Preisvorteil", preisvorteil.name());
        assertEquals(new BigDecimal("-2.00"), preisvorteil.singlePrice());
        assertEquals(1, preisvorteil.quantity());
        assertEquals(new BigDecimal("-2.00"), preisvorteil.price());

        final ParsedItem apfel = result.items().get(2);
        assertEquals("Apfel pink", apfel.name());
        assertEquals(new BigDecimal("2.09"), apfel.singlePrice());
        assertEquals(1, apfel.quantity());
        assertEquals(new BigDecimal("2.09"), apfel.price());
    }
}
