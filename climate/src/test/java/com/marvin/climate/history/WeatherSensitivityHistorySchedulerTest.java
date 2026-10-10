package com.marvin.climate.history;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class WeatherSensitivityHistorySchedulerTest {

    @Mock
    private WeatherSensitivityHistoryService historyService;

    @Test
    void onStartupSubscribesToRecord() {
        final AtomicInteger subscriptions = new AtomicInteger();
        when(historyService.record()).thenReturn(Mono.<Void>empty().doOnSubscribe(s -> subscriptions.incrementAndGet()));

        new WeatherSensitivityHistoryScheduler(historyService).onStartup();

        org.junit.jupiter.api.Assertions.assertEquals(1, subscriptions.get());
    }

    @Test
    void scheduledSubscribesToRecord() {
        final AtomicInteger subscriptions = new AtomicInteger();
        when(historyService.record()).thenReturn(Mono.<Void>empty().doOnSubscribe(s -> subscriptions.incrementAndGet()));

        new WeatherSensitivityHistoryScheduler(historyService).scheduledRecord();

        verify(historyService).record();
        org.junit.jupiter.api.Assertions.assertEquals(1, subscriptions.get());
    }
}
