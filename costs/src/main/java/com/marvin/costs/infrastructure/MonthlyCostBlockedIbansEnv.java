package com.marvin.costs.infrastructure;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Environment-variable-backed implementation of {@link Ibans} that provides blocked IBANs for monthly costs. */
@Component("monthlyCostBlockedIbans")
public class MonthlyCostBlockedIbansEnv implements Ibans {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonthlyCostBlockedIbansEnv.class);

    private final Set<String> blockedIbans;

    /**
     * Constructs a new {@code MonthlyCostBlockedIbansEnv} and initialises the blocked IBANs.
     *
     * @param ibansProperty the comma-separated IBANs sourced from the {@code monthly.cost.blocked-ibans} property
     */
    public MonthlyCostBlockedIbansEnv(@Value("${monthly.cost.blocked-ibans}") final String ibansProperty) {
        this.blockedIbans = initIbans(ibansProperty);
    }

    private Set<String> initIbans(final String property) {
        if (!StringUtils.hasText(property)) {
            LOGGER.info("Initialized monthly cost blocked IBANs with: {}!", Set.of());
            return Set.of();
        }
        final Set<String> ibans = Arrays.stream(property.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
        LOGGER.info("Initialized monthly cost blocked IBANs with: {}!", ibans);
        return ibans;
    }

    @Override
    public Set<String> getIbans() {
        return blockedIbans;
    }
}
