package com.marvin.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.entity.ExcludedDayEntity;
import com.marvin.nutrition.repository.ExcludedDayRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

/** Unit tests for {@link ExcludedDayService}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExcludedDayService Tests")
class ExcludedDayServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);

    @Mock
    private ExcludedDayRepository excludedDayRepository;

    @InjectMocks
    private ExcludedDayService excludedDayService;

    private ExcludedDayEntity entity(LocalDate date, String reason) {
        final ExcludedDayEntity entity = new ExcludedDayEntity();
        entity.setEntryDate(date);
        entity.setReason(reason);
        return entity;
    }

    @Test
    @DisplayName("findBetween maps the excluded days within the range")
    void findBetween_ReturnsMappedDays() {
        final LocalDate to = DAY.plusDays(5);
        when(excludedDayRepository.findByEntryDateBetweenOrderByEntryDateAsc(DAY, to))
                .thenReturn(List.of(entity(DAY, "Restaurant")));

        StepVerifier.create(excludedDayService.findBetween(DAY, to))
                .assertNext(days -> {
                    assertEquals(1, days.size());
                    assertEquals(DAY, days.get(0).date());
                    assertEquals("Restaurant", days.get(0).reason());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("exclude creates a new excluded day with a trimmed reason")
    void exclude_NewDay_CreatesEntity() {
        when(excludedDayRepository.findById(DAY)).thenReturn(Optional.empty());
        when(excludedDayRepository.save(any(ExcludedDayEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(excludedDayService.exclude(DAY, "  Urlaub  "))
                .assertNext(dto -> {
                    assertEquals(DAY, dto.date());
                    assertEquals("Urlaub", dto.reason());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("exclude updates the reason of an already excluded day and stores blank reasons as null")
    void exclude_ExistingDay_UpdatesReason() {
        when(excludedDayRepository.findById(DAY)).thenReturn(Optional.of(entity(DAY, "Restaurant")));
        when(excludedDayRepository.save(any(ExcludedDayEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(excludedDayService.exclude(DAY, " "))
                .assertNext(dto -> assertNull(dto.reason()))
                .verifyComplete();
    }

    @Test
    @DisplayName("include deletes an excluded day")
    void include_ExcludedDay_Deletes() {
        when(excludedDayRepository.existsById(DAY)).thenReturn(true);

        StepVerifier.create(excludedDayService.include(DAY)).verifyComplete();

        verify(excludedDayRepository).deleteById(DAY);
    }

    @Test
    @DisplayName("include is a no-op for a day that is not excluded")
    void include_NotExcluded_DoesNothing() {
        when(excludedDayRepository.existsById(DAY)).thenReturn(false);

        StepVerifier.create(excludedDayService.include(DAY)).verifyComplete();

        verify(excludedDayRepository, never()).deleteById(DAY);
    }
}
