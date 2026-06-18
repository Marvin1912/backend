package com.marvin.costs.infrastructure;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Environment-variable-backed implementation of {@link Ibans} that provides IBANs for salary imports. */
@Component("salaryImportIbans")
public class SalaryImportIbansEnv implements Ibans {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalaryImportIbansEnv.class);

    private final Set<String> salaryIbans;

    /**
     * Constructs a new {@code SalaryImportIbansEnv} and initialises the salary IBANs.
     *
     * @param ibansProperty the comma-separated IBANs sourced from the {@code salary.import.ibans} property
     */
    public SalaryImportIbansEnv(@Value("${salary.import.ibans}") final String ibansProperty) {
        this.salaryIbans = initIbans(ibansProperty);
    }

    private Set<String> initIbans(final String property) {
        if (!StringUtils.hasText(property)) {
            LOGGER.info("Initialized salary import IBANs with: {}!", Set.of());
            return Set.of();
        }
        final Set<String> ibans = Arrays.stream(property.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
        LOGGER.info("Initialized salary import IBANs with: {}!", ibans);
        return ibans;
    }

    @Override
    public Set<String> getIbans() {
        return salaryIbans;
    }
}
