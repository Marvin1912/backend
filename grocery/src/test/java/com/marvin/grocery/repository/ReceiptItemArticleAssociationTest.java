package com.marvin.grocery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ReceiptEntity;
import com.marvin.grocery.entity.ReceiptItemEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Repository integration test verifying the nullable {@code article} link on {@link ReceiptItemEntity}. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ReceiptItemArticleAssociationTest {

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
    void receiptItemCanBeSavedWithoutAnArticleLink() {
        final ReceiptItemEntity saved = saveReceiptWithItem(null);

        final Optional<ReceiptItemEntity> found = receiptItemRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getArticle()).isNull();
    }

    @Test
    void receiptItemCanBeLinkedToAnArticle() {
        final ArticleEntity article = articleRepository.save(newArticle("Tomaten", "tomaten"));

        final ReceiptItemEntity saved = saveReceiptWithItem(article);

        final Optional<ReceiptItemEntity> found = receiptItemRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getArticle()).isNotNull();
        assertThat(found.get().getArticle().getId()).isEqualTo(article.getId());
    }

    private ReceiptItemEntity saveReceiptWithItem(final ArticleEntity article) {
        final ReceiptEntity receipt = new ReceiptEntity();
        receipt.setReceiptDate(LocalDate.of(2026, 1, 1));
        receiptRepository.save(receipt);

        final ReceiptItemEntity item = new ReceiptItemEntity();
        item.setReceipt(receipt);
        item.setName("Tomaten");
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
