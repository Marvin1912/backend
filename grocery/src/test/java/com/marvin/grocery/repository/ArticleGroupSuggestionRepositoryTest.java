package com.marvin.grocery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.entity.ArticleGroupEntity;
import com.marvin.grocery.entity.ArticleGroupSuggestionEntity;
import com.marvin.grocery.entity.SuggestionSource;
import com.marvin.grocery.entity.SuggestionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Repository integration test for {@link ArticleGroupSuggestionEntity}, exercising the real grocery Flyway migrations. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ArticleGroupSuggestionRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private ArticleGroupSuggestionRepository articleGroupSuggestionRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleGroupRepository articleGroupRepository;

    @Test
    void findAllWithDetailsByStatusReturnsPendingSuggestionsWithArticleAndGroupInitialized() {
        final ArticleGroupEntity group = articleGroupRepository.save(newGroup("Dairy"));
        final ArticleEntity article = articleRepository.save(newArticle("Vollmilch", "vollmilch"));
        articleGroupSuggestionRepository.save(newSuggestion(article, group, 0.8, SuggestionStatus.PENDING));

        final List<ArticleGroupSuggestionEntity> pending =
                articleGroupSuggestionRepository.findAllWithDetailsByStatus(SuggestionStatus.PENDING);

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getArticle().getName()).isEqualTo("Vollmilch");
        assertThat(pending.get(0).getSuggestedGroup().getName()).isEqualTo("Dairy");
    }

    @Test
    void existsByArticleIdAndStatusReturnsTrueOnlyForMatchingStatus() {
        final ArticleGroupEntity group = articleGroupRepository.save(newGroup("Dairy"));
        final ArticleEntity article = articleRepository.save(newArticle("Butter", "butter"));
        articleGroupSuggestionRepository.save(newSuggestion(article, group, 0.8, SuggestionStatus.PENDING));

        assertThat(articleGroupSuggestionRepository.existsByArticleIdAndStatus(article.getId(), SuggestionStatus.PENDING))
                .isTrue();
        assertThat(articleGroupSuggestionRepository.existsByArticleIdAndStatus(article.getId(), SuggestionStatus.ACCEPTED))
                .isFalse();
    }

    private ArticleGroupEntity newGroup(String name) {
        final ArticleGroupEntity group = new ArticleGroupEntity();
        group.setName(name);
        return group;
    }

    private ArticleEntity newArticle(String name, String normalizedName) {
        final ArticleEntity article = new ArticleEntity();
        article.setName(name);
        article.setNormalizedName(normalizedName);
        return article;
    }

    private ArticleGroupSuggestionEntity newSuggestion(
            ArticleEntity article, ArticleGroupEntity group, double score, SuggestionStatus status) {
        final ArticleGroupSuggestionEntity suggestion = new ArticleGroupSuggestionEntity();
        suggestion.setArticle(article);
        suggestion.setSuggestedGroup(group);
        suggestion.setScore(score);
        suggestion.setSource(SuggestionSource.HEURISTIC);
        suggestion.setStatus(status);
        return suggestion;
    }
}
