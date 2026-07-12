package com.marvin.grocery.repository;

import com.marvin.grocery.entity.ReceiptItemEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ReceiptItemEntity}. */
@Repository
public interface ReceiptItemRepository extends JpaRepository<ReceiptItemEntity, Long> {

    /**
     * Finds all items belonging to the given receipt.
     *
     * @param receiptId the UUID of the parent receipt
     * @return list of items for the specified receipt
     */
    List<ReceiptItemEntity> findByReceiptId(UUID receiptId);

    /**
     * Finds a single item by its id that belongs to the given receipt.
     *
     * @param id        the item id
     * @param receiptId the UUID of the parent receipt
     * @return an Optional containing the item if it exists and belongs to the receipt
     */
    Optional<ReceiptItemEntity> findByIdAndReceiptId(Long id, UUID receiptId);

    /**
     * Returns all receipt items whose article is assigned to an article group, with their parent receipt,
     * article, and article group eagerly fetched, for article-group price-trend aggregation. Items without
     * an article, or whose article has no group assignment, are excluded.
     *
     * @return list of grouped receipt items with receipt, article, and article group initialized
     */
    @Query("SELECT i FROM ReceiptItemEntity i JOIN FETCH i.receipt JOIN FETCH i.article a JOIN FETCH a.articleGroup "
            + "WHERE i.article IS NOT NULL AND i.article.articleGroup IS NOT NULL")
    List<ReceiptItemEntity> findAllGroupedWithReceipt();

    /**
     * Returns all receipt items whose article belongs to the given article group, with their parent receipt,
     * article, and article group eagerly fetched.
     *
     * @param groupId the id of the article group to match
     * @return list of matching receipt items with receipt, article, and article group initialized
     */
    @Query("SELECT i FROM ReceiptItemEntity i JOIN FETCH i.receipt JOIN FETCH i.article a JOIN FETCH a.articleGroup "
            + "WHERE i.article.articleGroup.id = :groupId")
    List<ReceiptItemEntity> findAllByArticleGroupIdWithReceipt(@Param("groupId") Long groupId);

    /**
     * Returns the number of receipt items referencing the given article.
     *
     * @param articleId the id of the article to count receipt items for
     * @return the number of matching receipt items
     */
    long countByArticleId(Long articleId);

    /**
     * Returns the number of receipt items referencing each article that has at least one, grouped
     * by article id, so the full article list can be built without an N+1 query per article.
     *
     * @return one projection per article id that has at least one referencing receipt item
     */
    @Query("SELECT i.article.id AS articleId, COUNT(i) AS purchaseCount FROM ReceiptItemEntity i "
            + "WHERE i.article IS NOT NULL GROUP BY i.article.id")
    List<ArticlePurchaseCount> countPurchasesGroupedByArticle();
}
