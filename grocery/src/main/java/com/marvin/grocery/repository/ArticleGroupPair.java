package com.marvin.grocery.repository;

/**
 * Lightweight projection of an article-to-group pairing, e.g. a previously rejected suggestion.
 *
 * @param articleId the id of the article
 * @param groupId   the id of the article group
 */
public record ArticleGroupPair(Long articleId, Long groupId) {
}
