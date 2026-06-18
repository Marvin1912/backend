package com.marvin.costs.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MonthlyCostBlockedIbansEnv}. */
class MonthlyCostBlockedIbansEnvTest {

    @Test
    void shouldParseMultipleCommaSeparatedValues() {
        final MonthlyCostBlockedIbansEnv ibans = new MonthlyCostBlockedIbansEnv("DE01,DE02,DE03");

        assertEquals(Set.of("DE01", "DE02", "DE03"), ibans.getIbans());
    }

    @Test
    void shouldTrimWhitespaceAroundValues() {
        final MonthlyCostBlockedIbansEnv ibans = new MonthlyCostBlockedIbansEnv(" DE01 , DE02 ,DE03 ");

        assertEquals(Set.of("DE01", "DE02", "DE03"), ibans.getIbans());
    }

    @Test
    void shouldHandleSingleValueWithoutCommas() {
        final MonthlyCostBlockedIbansEnv ibans = new MonthlyCostBlockedIbansEnv("DE01");

        assertEquals(Set.of("DE01"), ibans.getIbans());
    }

    @Test
    void shouldReturnEmptySetForBlankInput() {
        final MonthlyCostBlockedIbansEnv ibans = new MonthlyCostBlockedIbansEnv("   ");

        assertTrue(ibans.getIbans().isEmpty());
    }

    @Test
    void shouldReturnEmptySetForEmptyInput() {
        final MonthlyCostBlockedIbansEnv ibans = new MonthlyCostBlockedIbansEnv("");

        assertTrue(ibans.getIbans().isEmpty());
    }

    @Test
    void shouldReturnEmptySetForNullInput() {
        final MonthlyCostBlockedIbansEnv ibans = new MonthlyCostBlockedIbansEnv(null);

        assertTrue(ibans.getIbans().isEmpty());
    }

    @Test
    void shouldImplementIbansInterface() {
        final Ibans ibans = new MonthlyCostBlockedIbansEnv("DE01");

        assertEquals(Set.of("DE01"), ibans.getIbans());
    }
}
