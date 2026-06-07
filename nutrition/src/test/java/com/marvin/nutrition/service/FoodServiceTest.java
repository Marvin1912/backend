package com.marvin.nutrition.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.nutrition.dto.FoodDTO;
import com.marvin.nutrition.entity.FoodEntity;
import com.marvin.nutrition.entity.FoodSource;
import com.marvin.nutrition.mapper.FoodMapper;
import com.marvin.nutrition.repository.FoodRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import reactor.test.StepVerifier;

/** Unit tests for {@link FoodService} covering CRUD, source defaulting, wildcard escaping, and error paths. */
@ExtendWith(MockitoExtension.class)
@DisplayName("FoodService Tests")
class FoodServiceTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private FoodMapper foodMapper;

    @InjectMocks
    private FoodService foodService;

    private UUID testId;
    private FoodEntity testEntity;
    private FoodDTO testDTO;

    /** Sets up shared fixtures for each test. */
    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

        testEntity = new FoodEntity();
        testEntity.setName("Chicken Breast");
        testEntity.setBrand("Brand A");
        testEntity.setKcalPer100(new BigDecimal("165.00"));
        testEntity.setProteinPer100(new BigDecimal("31.00"));
        testEntity.setCarbsPer100(new BigDecimal("0.00"));
        testEntity.setFatPer100(new BigDecimal("3.60"));
        testEntity.setFiberPer100(null);
        testEntity.setDefaultServingG(new BigDecimal("100.00"));
        testEntity.setSource(FoodSource.MANUAL);

        testDTO = new FoodDTO(
                testId,
                "Chicken Breast",
                "Brand A",
                new BigDecimal("165.00"),
                new BigDecimal("31.00"),
                new BigDecimal("0.00"),
                new BigDecimal("3.60"),
                null,
                new BigDecimal("100.00"),
                FoodSource.MANUAL
        );
    }

    // -----------------------------------------------------------------------
    // create — source defaulting
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("create defaults source to MANUAL when DTO source is null")
    void create_NullSource_DefaultsToManual() {
        final FoodDTO dtoWithNullSource = new FoodDTO(
                null, "Rice", null,
                new BigDecimal("130.00"), new BigDecimal("2.70"),
                new BigDecimal("28.00"), new BigDecimal("0.30"),
                null, new BigDecimal("100.00"), null
        );
        final FoodEntity entityWithNullSource = new FoodEntity();
        entityWithNullSource.setName("Rice");
        entityWithNullSource.setSource(null);

        final FoodEntity savedEntity = new FoodEntity();
        savedEntity.setName("Rice");
        savedEntity.setSource(FoodSource.MANUAL);

        when(foodMapper.toEntity(dtoWithNullSource)).thenReturn(entityWithNullSource);
        when(foodRepository.save(any(FoodEntity.class))).thenReturn(savedEntity);
        when(foodMapper.toDTO(savedEntity)).thenReturn(
                new FoodDTO(UUID.randomUUID(), "Rice", null,
                        new BigDecimal("130.00"), new BigDecimal("2.70"),
                        new BigDecimal("28.00"), new BigDecimal("0.30"),
                        null, new BigDecimal("100.00"), FoodSource.MANUAL)
        );

        StepVerifier.create(foodService.create(dtoWithNullSource))
                .assertNext(result -> {
                    assert result.source() == FoodSource.MANUAL;
                })
                .verifyComplete();

        verify(foodRepository).save(argThat(e -> e.getSource() == FoodSource.MANUAL));
    }

    @Test
    @DisplayName("create preserves a supplied source (PHOTO)")
    void create_SuppliedSource_Preserved() {
        final FoodDTO dtoWithPhoto = new FoodDTO(
                null, "Yogurt", "Danone",
                new BigDecimal("59.00"), new BigDecimal("10.00"),
                new BigDecimal("3.60"), new BigDecimal("0.10"),
                null, new BigDecimal("150.00"), FoodSource.PHOTO
        );
        final FoodEntity entityWithPhoto = new FoodEntity();
        entityWithPhoto.setName("Yogurt");
        entityWithPhoto.setSource(FoodSource.PHOTO);

        when(foodMapper.toEntity(dtoWithPhoto)).thenReturn(entityWithPhoto);
        when(foodRepository.save(entityWithPhoto)).thenReturn(entityWithPhoto);
        when(foodMapper.toDTO(entityWithPhoto)).thenReturn(
                new FoodDTO(UUID.randomUUID(), "Yogurt", "Danone",
                        new BigDecimal("59.00"), new BigDecimal("10.00"),
                        new BigDecimal("3.60"), new BigDecimal("0.10"),
                        null, new BigDecimal("150.00"), FoodSource.PHOTO)
        );

        StepVerifier.create(foodService.create(dtoWithPhoto))
                .assertNext(result -> {
                    assert result.source() == FoodSource.PHOTO;
                })
                .verifyComplete();

        verify(foodRepository).save(argThat(e -> e.getSource() == FoodSource.PHOTO));
    }

    // -----------------------------------------------------------------------
    // create — id never propagated from client
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("create passes entity with null id to save (mapper must ignore client id)")
    void create_ClientId_NotPropagatedToSave() {
        final FoodDTO dtoWithId = new FoodDTO(
                UUID.randomUUID(), "Oats", null,
                new BigDecimal("389.00"), new BigDecimal("17.00"),
                new BigDecimal("66.00"), new BigDecimal("7.00"),
                new BigDecimal("10.00"), new BigDecimal("40.00"), FoodSource.MANUAL
        );
        // Mapper correctly ignores the id (as enforced by @Mapping(target="id", ignore=true))
        final FoodEntity entityWithNullId = new FoodEntity();
        entityWithNullId.setSource(FoodSource.MANUAL);
        // id is null — mapper does not propagate it

        when(foodMapper.toEntity(dtoWithId)).thenReturn(entityWithNullId);
        when(foodRepository.save(entityWithNullId)).thenReturn(entityWithNullId);
        when(foodMapper.toDTO(entityWithNullId)).thenReturn(dtoWithId);

        StepVerifier.create(foodService.create(dtoWithId))
                .expectNextCount(1)
                .verifyComplete();

        // The entity reaching save must have a null id
        verify(foodRepository).save(argThat(e -> e.getId() == null));
    }

    // -----------------------------------------------------------------------
    // update — full field replacement
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("update copies all fields and replaces source, saves and returns DTO")
    void update_AllFields_CopiedAndSaved() {
        final FoodDTO updateDTO = new FoodDTO(
                null, "Chicken Breast Grilled", "Brand B",
                new BigDecimal("155.00"), new BigDecimal("32.00"),
                new BigDecimal("0.00"), new BigDecimal("3.00"),
                new BigDecimal("0.00"), new BigDecimal("120.00"), FoodSource.BARCODE
        );
        final FoodEntity existingEntity = new FoodEntity();
        existingEntity.setName("Chicken Breast");
        existingEntity.setSource(FoodSource.MANUAL);

        final FoodEntity savedEntity = new FoodEntity();
        savedEntity.setName("Chicken Breast Grilled");
        savedEntity.setSource(FoodSource.BARCODE);

        final FoodDTO savedDTO = new FoodDTO(
                testId, "Chicken Breast Grilled", "Brand B",
                new BigDecimal("155.00"), new BigDecimal("32.00"),
                new BigDecimal("0.00"), new BigDecimal("3.00"),
                new BigDecimal("0.00"), new BigDecimal("120.00"), FoodSource.BARCODE
        );

        when(foodRepository.findById(testId)).thenReturn(Optional.of(existingEntity));
        when(foodRepository.save(existingEntity)).thenReturn(savedEntity);
        when(foodMapper.toDTO(savedEntity)).thenReturn(savedDTO);

        StepVerifier.create(foodService.update(testId, updateDTO))
                .assertNext(result -> {
                    assert "Chicken Breast Grilled".equals(result.name());
                    assert result.source() == FoodSource.BARCODE;
                })
                .verifyComplete();

        verify(foodRepository).save(argThat(e ->
                "Chicken Breast Grilled".equals(e.getName())
                        && "Brand B".equals(e.getBrand())
                        && e.getSource() == FoodSource.BARCODE
        ));
    }

    @Test
    @DisplayName("update replaces source with MANUAL when incoming source is null")
    void update_NullSource_DefaultsToManual() {
        final FoodDTO updateDTO = new FoodDTO(
                null, "Oats", null,
                new BigDecimal("389.00"), new BigDecimal("17.00"),
                new BigDecimal("66.00"), new BigDecimal("7.00"),
                new BigDecimal("10.00"), new BigDecimal("40.00"), null
        );
        final FoodEntity existingEntity = new FoodEntity();
        existingEntity.setName("Oats Old");
        existingEntity.setSource(FoodSource.BARCODE);

        final FoodEntity savedEntity = new FoodEntity();
        savedEntity.setSource(FoodSource.MANUAL);

        when(foodRepository.findById(testId)).thenReturn(Optional.of(existingEntity));
        when(foodRepository.save(existingEntity)).thenReturn(savedEntity);
        when(foodMapper.toDTO(savedEntity)).thenReturn(
                new FoodDTO(testId, "Oats", null,
                        new BigDecimal("389.00"), new BigDecimal("17.00"),
                        new BigDecimal("66.00"), new BigDecimal("7.00"),
                        new BigDecimal("10.00"), new BigDecimal("40.00"), FoodSource.MANUAL)
        );

        StepVerifier.create(foodService.update(testId, updateDTO))
                .assertNext(result -> {
                    assert result.source() == FoodSource.MANUAL;
                })
                .verifyComplete();

        verify(foodRepository).save(argThat(e -> e.getSource() == FoodSource.MANUAL));
    }

    @Test
    @DisplayName("update throws NoSuchElementException when id is absent")
    void update_NotFound_ThrowsNoSuchElement() {
        final UUID unknownId = UUID.randomUUID();
        when(foodRepository.findById(unknownId)).thenReturn(Optional.empty());

        StepVerifier.create(foodService.update(unknownId, testDTO))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(foodRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // delete
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("delete throws NoSuchElementException when id is absent")
    void delete_NotFound_ThrowsNoSuchElement() {
        final UUID unknownId = UUID.randomUUID();
        when(foodRepository.existsById(unknownId)).thenReturn(false);

        StepVerifier.create(foodService.delete(unknownId))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(foodRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete removes entity and completes when present")
    void delete_Exists_DeletesAndCompletes() {
        when(foodRepository.existsById(testId)).thenReturn(true);

        StepVerifier.create(foodService.delete(testId))
                .verifyComplete();

        verify(foodRepository).deleteById(testId);
    }

    // -----------------------------------------------------------------------
    // findById
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findById throws NoSuchElementException when id is absent")
    void findById_NotFound_ThrowsNoSuchElement() {
        final UUID unknownId = UUID.randomUUID();
        when(foodRepository.findById(unknownId)).thenReturn(Optional.empty());

        StepVerifier.create(foodService.findById(unknownId))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    @DisplayName("findById returns DTO when entity exists")
    void findById_Found_ReturnsDTO() {
        when(foodRepository.findById(testId)).thenReturn(Optional.of(testEntity));
        when(foodMapper.toDTO(testEntity)).thenReturn(testDTO);

        StepVerifier.create(foodService.findById(testId))
                .expectNext(testDTO)
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // findAll — routing and wildcard escaping
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findAll with null q calls findAll(Sort), not searchByName")
    void findAll_NullQuery_CallsFindAllSort() {
        when(foodRepository.findAll(any(Sort.class))).thenReturn(List.of(testEntity));
        when(foodMapper.toDTOList(List.of(testEntity))).thenReturn(List.of(testDTO));

        StepVerifier.create(foodService.findAll(null))
                .expectNext(testDTO)
                .verifyComplete();

        verify(foodRepository).findAll(any(Sort.class));
        verify(foodRepository, never()).searchByName(any());
    }

    @Test
    @DisplayName("findAll with blank q calls findAll(Sort), not searchByName")
    void findAll_BlankQuery_CallsFindAllSort() {
        when(foodRepository.findAll(any(Sort.class))).thenReturn(List.of(testEntity));
        when(foodMapper.toDTOList(List.of(testEntity))).thenReturn(List.of(testDTO));

        StepVerifier.create(foodService.findAll("   "))
                .expectNext(testDTO)
                .verifyComplete();

        verify(foodRepository).findAll(any(Sort.class));
        verify(foodRepository, never()).searchByName(any());
    }

    @Test
    @DisplayName("findAll with non-blank q calls searchByName with LIKE-escaped string")
    void findAll_NonBlankQuery_CallsSearchByName() {
        when(foodRepository.searchByName("chicken")).thenReturn(List.of(testEntity));
        when(foodMapper.toDTOList(List.of(testEntity))).thenReturn(List.of(testDTO));

        StepVerifier.create(foodService.findAll("chicken"))
                .expectNext(testDTO)
                .verifyComplete();

        verify(foodRepository).searchByName("chicken");
        verify(foodRepository, never()).findAll(any(Sort.class));
    }

    @Test
    @DisplayName("findAll escapes percent wildcard in query before passing to searchByName")
    void findAll_QueryWithPercent_EscapesPercent() {
        when(foodRepository.searchByName(eq("50\\%"))).thenReturn(List.of());
        when(foodMapper.toDTOList(List.of())).thenReturn(List.of());

        StepVerifier.create(foodService.findAll("50%"))
                .verifyComplete();

        verify(foodRepository).searchByName("50\\%");
    }

    @Test
    @DisplayName("findAll escapes underscore wildcard in query before passing to searchByName")
    void findAll_QueryWithUnderscore_EscapesUnderscore() {
        when(foodRepository.searchByName(eq("fat\\_free"))).thenReturn(List.of());
        when(foodMapper.toDTOList(List.of())).thenReturn(List.of());

        StepVerifier.create(foodService.findAll("fat_free"))
                .verifyComplete();

        verify(foodRepository).searchByName("fat\\_free");
    }

    @Test
    @DisplayName("findAll escapes backslash in query before passing to searchByName")
    void findAll_QueryWithBackslash_EscapesBackslash() {
        when(foodRepository.searchByName(eq("a\\\\b"))).thenReturn(List.of());
        when(foodMapper.toDTOList(List.of())).thenReturn(List.of());

        StepVerifier.create(foodService.findAll("a\\b"))
                .verifyComplete();

        verify(foodRepository).searchByName("a\\\\b");
    }
}
