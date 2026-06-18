package com.marvin.costs.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SpecialCostBlockedIbansEnv}. */
class SpecialCostBlockedIbansEnvTest {

    @Test
    void shouldParseMultipleCommaSeparatedValues() {
        final SpecialCostBlockedIbansEnv ibans = new SpecialCostBlockedIbansEnv("DE01,DE02,DE03");

        assertEquals(Set.of("DE01", "DE02", "DE03"), ibans.getIbans());
    }

    @Test
    void shouldTrimWhitespaceAroundValues() {
        final SpecialCostBlockedIbansEnv ibans = new SpecialCostBlockedIbansEnv(" DE01 , DE02 ,DE03 ");

        assertEquals(Set.of("DE01", "DE02", "DE03"), ibans.getIbans());
    }

    @Test
    void shouldHandleSingleValueWithoutCommas() {
        final SpecialCostBlockedIbansEnv ibans = new SpecialCostBlockedIbansEnv("DE01");

        assertEquals(Set.of("DE01"), ibans.getIbans());
    }

    @Test
    void shouldReturnEmptySetForBlankInput() {
        final SpecialCostBlockedIbansEnv ibans = new SpecialCostBlockedIbansEnv("   ");

        assertTrue(ibans.getIbans().isEmpty());
    }

    @Test
    void shouldReturnEmptySetForEmptyInput() {
        final SpecialCostBlockedIbansEnv ibans = new SpecialCostBlockedIbansEnv("");

        assertTrue(ibans.getIbans().isEmpty());
    }

    @Test
    void shouldReturnEmptySetForNullInput() {
        final SpecialCostBlockedIbansEnv ibans = new SpecialCostBlockedIbansEnv(null);

        assertTrue(ibans.getIbans().isEmpty());
    }

    @Test
    void shouldImplementIbansInterface() {
        final Ibans ibans = new SpecialCostBlockedIbansEnv("DE01");

        assertEquals(Set.of("DE01"), ibans.getIbans());
    }
}
