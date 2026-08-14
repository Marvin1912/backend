package com.marvin.grocery.service;

import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.matching.ArticleGroupSuggestionService;
import com.marvin.grocery.repository.ArticleRepository;
import com.marvin.grocery.util.ArticleNameNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds or creates {@link ArticleEntity} rows by normalized name, so receipt items imported from
 * OCR or added manually can be linked to a stable article identity instead of only free-text names.
 */
@Service("groceryArticleService")
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleGroupSuggestionService articleGroupSuggestionService;

    /**
     * Creates a new ArticleService with the required dependencies.
     *
     * @param articleRepository             the JPA repository for articles
     * @param articleGroupSuggestionService the service used to auto-match newly created articles to a group
     */
    public ArticleService(ArticleRepository articleRepository, ArticleGroupSuggestionService articleGroupSuggestionService) {
        this.articleRepository = articleRepository;
        this.articleGroupSuggestionService = articleGroupSuggestionService;
    }

    /**
     * Finds the article matching the normalized form of the given name, or creates and persists a
     * new one if none exists yet. Newly created articles are run through the automated group matcher.
     *
     * @param name the raw (non-normalized) product name
     * @return the existing or newly created article entity
     */
    @Transactional
    public ArticleEntity findOrCreate(String name) {
        final String normalizedName = ArticleNameNormalizer.normalize(name);
        return articleRepository.findByNormalizedName(normalizedName)
                .orElseGet(() -> createAndMatchArticle(name, normalizedName));
    }

    private ArticleEntity createAndMatchArticle(String name, String normalizedName) {
        final ArticleEntity created = createArticle(name, normalizedName);
        articleGroupSuggestionService.matchNewArticle(created);
        return created;
    }

    private ArticleEntity createArticle(String name, String normalizedName) {
        final ArticleEntity article = new ArticleEntity();
        article.setName(name);
        article.setNormalizedName(normalizedName);
        return articleRepository.save(article);
    }
}
