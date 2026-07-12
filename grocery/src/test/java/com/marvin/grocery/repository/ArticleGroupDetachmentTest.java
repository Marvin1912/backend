package com.marvin.grocery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ArticleGroupEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Repository integration test verifying that {@link ArticleRepository#clearArticleGroup(Long)}
 * decouples articles from a group without deleting them, mirroring the codebase's Flyway migrations.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ArticleGroupDetachmentTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleGroupRepository articleGroupRepository;

    @Test
    @Transactional
    void clearArticleGroupNullsGroupReferenceWithoutDeletingArticle() {
        final ArticleGroupEntity group = articleGroupRepository.save(newGroup("Vegetables"));
        final ArticleEntity article = newArticle("Karotte", "karotte");
        article.setArticleGroup(group);
        final ArticleEntity saved = articleRepository.saveAndFlush(article);

        articleRepository.clearArticleGroup(group.getId());
        articleRepository.flush();

        final Optional<ArticleEntity> found = articleRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getArticleGroup()).isNull();
    }

    @Test
    void clearArticleGroupIsNoOpWhenNoArticleReferencesTheGroup() {
        final ArticleGroupEntity group = articleGroupRepository.save(newGroup("Empty Group"));

        articleRepository.clearArticleGroup(group.getId());

        assertThat(articleGroupRepository.findById(group.getId())).isPresent();
    }

    private ArticleEntity newArticle(final String name, final String normalizedName) {
        final ArticleEntity article = new ArticleEntity();
        article.setName(name);
        article.setNormalizedName(normalizedName);
        return article;
    }

    private ArticleGroupEntity newGroup(final String name) {
        final ArticleGroupEntity group = new ArticleGroupEntity();
        group.setName(name);
        return group;
    }
}
