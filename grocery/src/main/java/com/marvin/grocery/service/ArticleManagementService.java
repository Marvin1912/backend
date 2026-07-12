package com.marvin.grocery.service;

import com.marvin.grocery.dto.ArticleDTO;
import com.marvin.grocery.dto.ArticleGroupDTO;
import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ArticleGroupEntity;
import com.marvin.grocery.mapper.ArticleGroupMapper;
import com.marvin.grocery.repository.ArticleGroupRepository;
import com.marvin.grocery.repository.ArticlePurchaseCount;
import com.marvin.grocery.repository.ArticleRepository;
import com.marvin.grocery.repository.ReceiptItemRepository;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Orchestrates reactive read/write access to articles and article groups for the management API,
 * wrapping blocking JPA calls onto the bounded-elastic scheduler.
 */
@Service
public class ArticleManagementService {

    private final ArticleRepository articleRepository;
    private final ArticleGroupRepository articleGroupRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final ArticleGroupDeletionService articleGroupDeletionService;
    private final ArticleGroupMapper articleGroupMapper;

    /**
     * Creates a new ArticleManagementService with all required dependencies.
     *
     * @param articleRepository           the JPA repository for articles
     * @param articleGroupRepository      the JPA repository for article groups
     * @param receiptItemRepository       the JPA repository for receipt items
     * @param articleGroupDeletionService the service handling atomic group deletion and detachment
     * @param articleGroupMapper          the MapStruct mapper for article group DTO conversion
     */
    public ArticleManagementService(
            ArticleRepository articleRepository,
            ArticleGroupRepository articleGroupRepository,
            ReceiptItemRepository receiptItemRepository,
            ArticleGroupDeletionService articleGroupDeletionService,
            ArticleGroupMapper articleGroupMapper) {
        this.articleRepository = articleRepository;
        this.articleGroupRepository = articleGroupRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.articleGroupDeletionService = articleGroupDeletionService;
        this.articleGroupMapper = articleGroupMapper;
    }

    /**
     * Returns all articles with their group assignment and purchase count.
     *
     * @return a Flux emitting one article DTO per stored article
     */
    public Flux<ArticleDTO> findAllArticles() {
        return Mono.fromCallable(() -> {
            final Map<Long, Long> purchaseCounts = receiptItemRepository.countPurchasesGroupedByArticle().stream()
                    .collect(Collectors.toMap(ArticlePurchaseCount::getArticleId, ArticlePurchaseCount::getPurchaseCount));
            return articleRepository.findAllWithGroup().stream()
                    .map(article -> toArticleDTO(article, purchaseCounts.getOrDefault(article.getId(), 0L)))
                    .toList();
        }).subscribeOn(Schedulers.boundedElastic()).flatMapIterable(Function.identity());
    }

    /**
     * Assigns the given article to the given group, or clears the assignment when groupId is null.
     * Emits {@link NoSuchElementException} if the article does not exist, or if a non-null groupId
     * does not reference an existing group.
     *
     * @param articleId the id of the article to update
     * @param groupId   the id of the group to assign, or null to clear the assignment
     * @return a Mono emitting the updated article DTO
     */
    public Mono<ArticleDTO> assignGroup(Long articleId, Long groupId) {
        return Mono.fromCallable(() -> {
            final ArticleEntity article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new NoSuchElementException("Article not found: " + articleId));
            final ArticleGroupEntity group = resolveGroup(groupId);
            article.setArticleGroup(group);
            final ArticleEntity saved = articleRepository.save(article);
            final long purchaseCount = receiptItemRepository.countByArticleId(saved.getId());
            return toArticleDTO(saved, group, purchaseCount);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Creates a new article group with the given name.
     *
     * @param name the display name of the new group
     * @return a Mono emitting the created group DTO
     */
    public Mono<ArticleGroupDTO> createGroup(String name) {
        return Mono.fromCallable(() -> {
            final ArticleGroupEntity group = new ArticleGroupEntity();
            group.setName(name);
            return articleGroupMapper.toArticleGroupDTO(articleGroupRepository.save(group));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Renames the article group with the given id.
     * Emits {@link NoSuchElementException} if no group with that id exists.
     *
     * @param id   the id of the group to rename
     * @param name the new display name
     * @return a Mono emitting the updated group DTO
     */
    public Mono<ArticleGroupDTO> renameGroup(Long id, String name) {
        return Mono.fromCallable(() -> {
            final ArticleGroupEntity group = articleGroupRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Article group not found: " + id));
            group.setName(name);
            return articleGroupMapper.toArticleGroupDTO(articleGroupRepository.save(group));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes the article group with the given id, decoupling (not cascade-deleting) any articles
     * that were assigned to it.
     * Emits {@link NoSuchElementException} if no group with that id exists.
     *
     * @param id the id of the group to delete
     * @return an empty Mono on success, or an error Mono if the group does not exist
     */
    public Mono<Void> deleteGroup(Long id) {
        return Mono.fromCallable(() -> {
            articleGroupDeletionService.deleteAndDetach(id);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private ArticleGroupEntity resolveGroup(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return articleGroupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Article group not found: " + groupId));
    }

    private ArticleDTO toArticleDTO(ArticleEntity article, long purchaseCount) {
        return toArticleDTO(article, article.getArticleGroup(), purchaseCount);
    }

    private ArticleDTO toArticleDTO(ArticleEntity article, ArticleGroupEntity group, long purchaseCount) {
        return new ArticleDTO(
                article.getId(),
                article.getNormalizedName(),
                article.getName(),
                group == null ? null : group.getId(),
                group == null ? null : group.getName(),
                purchaseCount);
    }
}
