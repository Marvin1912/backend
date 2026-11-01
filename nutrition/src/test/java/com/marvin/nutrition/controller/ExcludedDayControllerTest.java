package com.marvin.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.ExcludeDayRequest;
import com.marvin.nutrition.dto.ExcludedDayDTO;
import com.marvin.nutrition.service.ExcludedDayService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for {@link ExcludedDayController}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExcludedDayController Tests")
class ExcludedDayControllerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);

    @Mock
    private ExcludedDayService excludedDayService;

    @InjectMocks
    private ExcludedDayController excludedDayController;

    @Test
    @DisplayName("getExcludedDays returns the excluded days within the range")
    void getExcludedDays_ReturnsList() {
        final LocalDate to = DAY.plusDays(1);
        final ExcludedDayDTO dto = new ExcludedDayDTO(DAY, "Restaurant");
        when(excludedDayService.findBetween(DAY, to)).thenReturn(Mono.just(List.of(dto)));

        StepVerifier.create(excludedDayController.getExcludedDays(DAY, to))
                .assertNext(days -> assertEquals(List.of(dto), days))
                .verifyComplete();
    }

    @Test
    @DisplayName("excludeDay returns 200 with the excluded day")
    void excludeDay_Returns200() {
        final ExcludedDayDTO dto = new ExcludedDayDTO(DAY, "Urlaub");
        when(excludedDayService.exclude(DAY, "Urlaub")).thenReturn(Mono.just(dto));

        StepVerifier.create(excludedDayController.excludeDay(DAY, new ExcludeDayRequest("Urlaub")))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(dto, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("includeDay returns 204 No Content")
    void includeDay_Returns204() {
        when(excludedDayService.include(DAY)).thenReturn(Mono.empty());

        StepVerifier.create(excludedDayController.includeDay(DAY))
                .assertNext(response -> assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode()))
                .verifyComplete();

        verify(excludedDayService).include(DAY);
    }

    @Test
    @DisplayName("excludeDay without a body excludes the day without a reason")
    void excludeDay_NoBody_ExcludesWithoutReason() {
        final ExcludedDayDTO dto = new ExcludedDayDTO(DAY, null);
        when(excludedDayService.exclude(DAY, null)).thenReturn(Mono.just(dto));

        StepVerifier.create(excludedDayController.excludeDay(DAY, null))
                .assertNext(response -> assertEquals(dto, response.getBody()))
                .verifyComplete();
    }
}
