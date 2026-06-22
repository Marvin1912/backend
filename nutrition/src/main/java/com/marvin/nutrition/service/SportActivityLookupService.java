package com.marvin.nutrition.service;

import com.marvin.nutrition.dto.SportActivityDTO;
import com.marvin.nutrition.mapper.SportActivityMapper;
import com.marvin.nutrition.repository.SportActivityRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Owns read access to sport activities for day summary computation, mapping persisted
 * {@link com.marvin.nutrition.entity.SportActivityEntity} rows to {@link SportActivityDTO}.
 *
 * <p>All methods in this service perform blocking repository access and must only be called from
 * a thread already running on a blocking-friendly scheduler (e.g. {@link reactor.core.scheduler.Schedulers#boundedElastic()}).</p>
 */
@Service
public class SportActivityLookupService {

    private final SportActivityRepository sportActivityRepository;
    private final SportActivityMapper sportActivityMapper;

    /**
     * Creates a new SportActivityLookupService with the required dependencies.
     *
     * @param sportActivityRepository JPA repository for sport activities
     * @param sportActivityMapper     MapStruct mapper for entity/DTO conversion
     */
    public SportActivityLookupService(
            SportActivityRepository sportActivityRepository, SportActivityMapper sportActivityMapper) {
        this.sportActivityRepository = sportActivityRepository;
        this.sportActivityMapper = sportActivityMapper;
    }

    /**
     * Returns all sport activities logged for the given date, mapped to DTOs.
     *
     * @param date the date to query
     * @return the activities logged for that date, in creation order
     */
    public List<SportActivityDTO> findByDate(LocalDate date) {
        return sportActivityRepository.findByEntryDateOrderByCreationDateAsc(date).stream()
                .map(sportActivityMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Returns all sport activities logged within the given date range (inclusive), grouped by date.
     *
     * @param from the first date to include
     * @param to   the last date to include
     * @return the activities within the range, keyed by date and ordered by creation time within each date
     */
    public Map<LocalDate, List<SportActivityDTO>> findByDateRangeGroupedByDate(LocalDate from, LocalDate to) {
        return sportActivityRepository.findByEntryDateBetweenOrderByEntryDateAscCreationDateAsc(from, to).stream()
                .map(sportActivityMapper::toDTO)
                .collect(Collectors.groupingBy(SportActivityDTO::entryDate));
    }
}
