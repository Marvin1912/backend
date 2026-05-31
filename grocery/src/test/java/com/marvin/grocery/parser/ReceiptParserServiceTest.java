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
    @DisplayName("Should parse a standard German receipt line")
    void parse_StandardGermanLine_ReturnsItem() {
        final String text = "Vollmilch 3,5%  1,09 A";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(1, result.items().size());
        assertEquals("Vollmilch 3,5%", result.items().get(0).name());
        assertEquals(new BigDecimal("1.09"), result.items().get(0).price());
    }

    @Test
    @DisplayName("Should parse an item with a multi-word name")
    void parse_MultiWordName_ReturnsItem() {
        final String text = "Bio Hafer Flocken  2,49 B";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(1, result.items().size());
        assertEquals("Bio Hafer Flocken", result.items().get(0).name());
        assertEquals(new BigDecimal("2.49"), result.items().get(0).price());
    }

    @Test
    @DisplayName("Should parse a price with a period decimal separator")
    void parse_PeriodDecimalSeparator_ReturnsItem() {
        final String text = "Butter  1.79";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(1, result.items().size());
        assertEquals("Butter", result.items().get(0).name());
        assertEquals(new BigDecimal("1.79"), result.items().get(0).price());
    }

    @Test
    @DisplayName("Should exclude total lines from items")
    void parse_TotalLine_IsExcluded() {
        final String text = "Apfel  0,89 A\nGesamt  0,89\nSumme  0,89";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(1, result.items().size());
        assertEquals("Apfel", result.items().get(0).name());
    }

    @Test
    @DisplayName("Should extract date in DD.MM.YYYY format")
    void parse_DateLine_ExtractsDate() {
        final String text = "Datum: 15.03.2024\nMilch  0,99 A";

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
        final String text = "15.03.2024\nVollmilch  1,09 A\nButter  1,79 B\nBrot  2,49";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(3, result.items().size());
        assertEquals(LocalDate.of(2024, 3, 15), result.receiptDate());
    }

    @Test
    @DisplayName("Should exclude 'zu zahlen' total line")
    void parse_ZuZahlenLine_IsExcluded() {
        final String text = "Joghurt  0,79\nZu zahlen  0,79";

        final ParsedReceipt result = parserService.parse(text);

        assertEquals(1, result.items().size());
        assertEquals("Joghurt", result.items().get(0).name());
    }
}
