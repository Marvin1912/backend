package com.marvin.grocery.service;

import com.marvin.grocery.repository.ArticleGroupRepository;
import com.marvin.grocery.repository.ArticleRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes article groups while atomically decoupling (never cascade-deleting) the articles that
 * reference them, so receipt items keep their article link intact.
 */
@Service
public class ArticleGroupDeletionService {

    private final ArticleGroupRepository articleGroupRepository;
    private final ArticleRepository articleRepository;

    /**
     * Creates a new ArticleGroupDeletionService with the required repositories.
     *
     * @param articleGroupRepository the JPA repository for article groups
     * @param articleRepository      the JPA repository for articles
     */
    public ArticleGroupDeletionService(ArticleGroupRepository articleGroupRepository, ArticleRepository articleRepository) {
        this.articleGroupRepository = articleGroupRepository;
        this.articleRepository = articleRepository;
    }

    /**
     * Deletes the article group with the given id after clearing the group assignment on every
     * article that references it. Both steps run in a single transaction, so articles and receipt
     * items are never cascade-deleted.
     *
     * @param id the id of the article group to delete
     */
    @Transactional
    public void deleteAndDetach(Long id) {
        if (!articleGroupRepository.existsById(id)) {
            throw new NoSuchElementException("Article group not found: " + id);
        }
        articleRepository.clearArticleGroup(id);
        articleGroupRepository.deleteById(id);
    }
}
