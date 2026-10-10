package com.marvin.climate.history;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the hourly recording of the weather sensitivity assessment.
 */
@Component
public class WeatherSensitivityHistoryScheduler {

    private final WeatherSensitivityHistoryService historyService;

    /**
     * Creates the scheduler.
     *
     * @param historyService the history service
     */
    public WeatherSensitivityHistoryScheduler(final WeatherSensitivityHistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * Records once on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        historyService.record().block();
    }

    /**
     * Records at the start of every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduledRecord() {
        historyService.record().block();
    }
}
