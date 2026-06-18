package com.marvin.costs.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SalaryImportIbansEnv}. */
class SalaryImportIbansEnvTest {

    @Test
    void shouldParseMultipleCommaSeparatedValues() {
        final SalaryImportIbansEnv ibans = new SalaryImportIbansEnv("DE01,DE02,DE03");

        assertEquals(Set.of("DE01", "DE02", "DE03"), ibans.getIbans());
    }

    @Test
    void shouldTrimWhitespaceAroundValues() {
        final SalaryImportIbansEnv ibans = new SalaryImportIbansEnv(" DE01 , DE02 ,DE03 ");

        assertEquals(Set.of("DE01", "DE02", "DE03"), ibans.getIbans());
    }

    @Test
    void shouldHandleSingleValueWithoutCommas() {
        final SalaryImportIbansEnv ibans = new SalaryImportIbansEnv("DE01");

        assertEquals(Set.of("DE01"), ibans.getIbans());
    }

    @Test
    void shouldReturnEmptySetForBlankInput() {
        final SalaryImportIbansEnv ibans = new SalaryImportIbansEnv("   ");

        assertTrue(ibans.getIbans().isEmpty());
    }

    @Test
    void shouldReturnEmptySetForEmptyInput() {
        final SalaryImportIbansEnv ibans = new SalaryImportIbansEnv("");

        assertTrue(ibans.getIbans().isEmpty());
    }

    @Test
    void shouldReturnEmptySetForNullInput() {
        final SalaryImportIbansEnv ibans = new SalaryImportIbansEnv(null);

        assertTrue(ibans.getIbans().isEmpty());
    }

    @Test
    void shouldImplementIbansInterface() {
        final Ibans ibans = new SalaryImportIbansEnv("DE01");

        assertEquals(Set.of("DE01"), ibans.getIbans());
    }
}
