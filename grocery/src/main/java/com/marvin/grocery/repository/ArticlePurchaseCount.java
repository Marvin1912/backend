package com.marvin.grocery.repository;

/**
 * Spring Data projection pairing an article id with the number of receipt items referencing it.
 */
public interface ArticlePurchaseCount {

    /**
     * Returns the id of the article being counted.
     *
     * @return the article id
     */
    Long getArticleId();

    /**
     * Returns the number of receipt items referencing the article.
     *
     * @return the purchase count
     */
    long getPurchaseCount();
}
