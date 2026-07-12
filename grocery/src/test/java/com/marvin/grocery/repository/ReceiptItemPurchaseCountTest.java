package com.marvin.grocery.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ReceiptEntity;
import com.marvin.grocery.entity.ReceiptItemEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Repository integration test for the purchase-count queries on {@link ReceiptItemRepository},
 * exercising the real grocery Flyway migrations.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ReceiptItemPurchaseCountTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private ReceiptItemRepository receiptItemRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Test
    void countByArticleIdReturnsNumberOfReferencingReceiptItems() {
        final ArticleEntity article = articleRepository.save(newArticle("Tomaten", "tomaten"));
        saveReceiptWithItem(article);
        saveReceiptWithItem(article);

        final long count = receiptItemRepository.countByArticleId(article.getId());

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countPurchasesGroupedByArticleOmitsArticlesWithoutReceiptItems() {
        final ArticleEntity purchased = articleRepository.save(newArticle("Gurke", "gurke"));
        articleRepository.save(newArticle("Never bought", "never bought"));
        saveReceiptWithItem(purchased);

        final List<ArticlePurchaseCount> counts = receiptItemRepository.countPurchasesGroupedByArticle();

        assertThat(counts)
                .extracting(ArticlePurchaseCount::getArticleId, ArticlePurchaseCount::getPurchaseCount)
                .containsExactly(tuple(purchased.getId(), 1L));
    }

    private ReceiptItemEntity saveReceiptWithItem(final ArticleEntity article) {
        final ReceiptEntity receipt = new ReceiptEntity();
        receipt.setReceiptDate(LocalDate.of(2026, 1, 1));
        receiptRepository.save(receipt);

        final ReceiptItemEntity item = new ReceiptItemEntity();
        item.setReceipt(receipt);
        item.setName(article.getName());
        item.setPrice(BigDecimal.valueOf(1.99));
        item.setSinglePrice(BigDecimal.valueOf(1.99));
        item.setQuantity(1);
        item.setArticle(article);
        return receiptItemRepository.save(item);
    }

    private ArticleEntity newArticle(final String name, final String normalizedName) {
        final ArticleEntity article = new ArticleEntity();
        article.setName(name);
        article.setNormalizedName(normalizedName);
        return article;
    }
}
