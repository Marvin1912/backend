package com.marvin.grocery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.grocery.dto.ArticleDTO;
import com.marvin.grocery.dto.ArticleGroupDTO;
import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ArticleGroupEntity;
import com.marvin.grocery.mapper.ArticleGroupMapper;
import com.marvin.grocery.repository.ArticleGroupRepository;
import com.marvin.grocery.repository.ArticlePurchaseCount;
import com.marvin.grocery.repository.ArticleRepository;
import com.marvin.grocery.repository.ReceiptItemRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleManagementService Tests")
class ArticleManagementServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleGroupRepository articleGroupRepository;

    @Mock
    private ReceiptItemRepository receiptItemRepository;

    @Mock
    private ArticleGroupDeletionService articleGroupDeletionService;

    @Mock
    private ArticleGroupMapper articleGroupMapper;

    @InjectMocks
    private ArticleManagementService articleManagementService;

    private ArticleGroupEntity group;
    private ArticleEntity article;

    @BeforeEach
    void setUp() {
        group = new ArticleGroupEntity();
        group.setId(3L);
        group.setName("Dairy");

        article = new ArticleEntity();
        article.setId(1L);
        article.setName("Vollmilch");
        article.setNormalizedName("vollmilch");
    }

    @Test
    @DisplayName("Should list all articles with group info and purchase count")
    void findAllArticles_ReturnsArticlesWithGroupAndPurchaseCount() {
        article.setArticleGroup(group);
        when(articleRepository.findAll()).thenReturn(List.of(article));
        when(receiptItemRepository.countPurchasesGroupedByArticle())
                .thenReturn(List.of(countOf(1L, 5L)));

        final Flux<ArticleDTO> result = articleManagementService.findAllArticles();

        StepVerifier.create(result)
                .assertNext(dto -> {
                    assertEquals(1L, dto.id());
                    assertEquals("vollmilch", dto.normalizedName());
                    assertEquals("Vollmilch", dto.name());
                    assertEquals(3L, dto.groupId());
                    assertEquals("Dairy", dto.groupName());
                    assertEquals(5L, dto.purchaseCount());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should default purchase count to zero and group fields to null when unassigned")
    void findAllArticles_NoGroupNoPurchases_DefaultsToZeroAndNull() {
        when(articleRepository.findAll()).thenReturn(List.of(article));
        when(receiptItemRepository.countPurchasesGroupedByArticle()).thenReturn(List.of());

        final Flux<ArticleDTO> result = articleManagementService.findAllArticles();

        StepVerifier.create(result)
                .assertNext(dto -> {
                    assertEquals(1L, dto.id());
                    assertEquals(0L, dto.purchaseCount());
                    assertNull(dto.groupId());
                    assertNull(dto.groupName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should assign the article to the given group")
    void assignGroup_ArticleAndGroupExist_AssignsGroup() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleGroupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(articleRepository.save(any(ArticleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptItemRepository.countByArticleId(1L)).thenReturn(2L);

        final Mono<ArticleDTO> result = articleManagementService.assignGroup(1L, 3L);

        StepVerifier.create(result)
                .assertNext(dto -> {
                    assertEquals(1L, dto.id());
                    assertEquals(3L, dto.groupId());
                    assertEquals("Dairy", dto.groupName());
                    assertEquals(2L, dto.purchaseCount());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should clear the group assignment when groupId is null")
    void assignGroup_NullGroupId_ClearsAssignment() {
        article.setArticleGroup(group);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(ArticleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptItemRepository.countByArticleId(1L)).thenReturn(0L);

        final Mono<ArticleDTO> result = articleManagementService.assignGroup(1L, null);

        StepVerifier.create(result)
                .assertNext(dto -> {
                    assertNull(dto.groupId());
                    assertNull(dto.groupName());
                })
                .verifyComplete();
        verify(articleGroupRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should emit NoSuchElementException when the article does not exist")
    void assignGroup_ArticleNotFound_EmitsError() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());

        final Mono<ArticleDTO> result = articleManagementService.assignGroup(99L, 3L);

        StepVerifier.create(result)
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    @DisplayName("Should emit NoSuchElementException when the requested group does not exist")
    void assignGroup_GroupNotFound_EmitsError() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleGroupRepository.findById(42L)).thenReturn(Optional.empty());

        final Mono<ArticleDTO> result = articleManagementService.assignGroup(1L, 42L);

        StepVerifier.create(result)
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    @DisplayName("Should create and map a new article group")
    void createGroup_CreatesAndReturnsMappedGroup() {
        when(articleGroupRepository.save(any(ArticleGroupEntity.class))).thenReturn(group);
        when(articleGroupMapper.toArticleGroupDTO(group)).thenReturn(new ArticleGroupDTO(3L, "Dairy"));

        final Mono<ArticleGroupDTO> result = articleManagementService.createGroup("Dairy");

        StepVerifier.create(result)
                .assertNext(dto -> assertEquals("Dairy", dto.name()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should rename an existing group")
    void renameGroup_GroupExists_RenamesAndReturnsMappedGroup() {
        when(articleGroupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(articleGroupRepository.save(any(ArticleGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(articleGroupMapper.toArticleGroupDTO(any(ArticleGroupEntity.class)))
                .thenAnswer(invocation -> {
                    final ArticleGroupEntity entity = invocation.getArgument(0);
                    return new ArticleGroupDTO(entity.getId(), entity.getName());
                });

        final Mono<ArticleGroupDTO> result = articleManagementService.renameGroup(3L, "Milk Products");

        StepVerifier.create(result)
                .assertNext(dto -> assertEquals("Milk Products", dto.name()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit NoSuchElementException when renaming an unknown group")
    void renameGroup_GroupNotFound_EmitsError() {
        when(articleGroupRepository.findById(99L)).thenReturn(Optional.empty());

        final Mono<ArticleGroupDTO> result = articleManagementService.renameGroup(99L, "New Name");

        StepVerifier.create(result)
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    @DisplayName("Should delegate group deletion to the deletion service")
    void deleteGroup_DelegatesToDeletionService() {
        final Mono<Void> result = articleManagementService.deleteGroup(3L);

        StepVerifier.create(result).verifyComplete();

        verify(articleGroupDeletionService).deleteAndDetach(3L);
    }

    @Test
    @DisplayName("Should propagate NoSuchElementException from the deletion service")
    void deleteGroup_UnknownGroup_PropagatesError() {
        doThrow(new NoSuchElementException("Article group not found: 99"))
                .when(articleGroupDeletionService).deleteAndDetach(99L);

        final Mono<Void> result = articleManagementService.deleteGroup(99L);

        StepVerifier.create(result)
                .expectError(NoSuchElementException.class)
                .verify();
    }

    private ArticlePurchaseCount countOf(final Long articleId, final long purchaseCount) {
        return new ArticlePurchaseCount() {
            @Override
            public Long getArticleId() {
                return articleId;
            }

            @Override
            public long getPurchaseCount() {
                return purchaseCount;
            }
        };
    }
}
