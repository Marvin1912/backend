package com.marvin.grocery.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ArticleGroupEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Repository integration test for {@link ArticleEntity}, exercising the real grocery Flyway migrations. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ArticleRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleGroupRepository articleGroupRepository;

    @Test
    void findByNormalizedNameReturnsMatchingArticle() {
        articleRepository.save(newArticle("Tomaten rot", "tomaten rot"));

        final Optional<ArticleEntity> found = articleRepository.findByNormalizedName("tomaten rot");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Tomaten rot");
    }

    @Test
    void findByNormalizedNameReturnsEmptyWhenNoArticleMatches() {
        final Optional<ArticleEntity> found = articleRepository.findByNormalizedName("does-not-exist");

        assertThat(found).isEmpty();
    }

    @Test
    void savingTwoArticlesWithSameNormalizedNameViolatesUniqueConstraint() {
        articleRepository.saveAndFlush(newArticle("Gurke", "gurke"));

        assertThatThrownBy(() -> articleRepository.saveAndFlush(newArticle("GURKE", "gurke")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void articleCanBeLinkedToAnArticleGroup() {
        final ArticleGroupEntity group = articleGroupRepository.save(newGroup("Vegetables"));
        final ArticleEntity article = newArticle("Karotte", "karotte");
        article.setArticleGroup(group);

        final ArticleEntity saved = articleRepository.save(article);

        final Optional<ArticleEntity> found = articleRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getArticleGroup()).isNotNull();
        assertThat(found.get().getArticleGroup().getId()).isEqualTo(group.getId());
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
