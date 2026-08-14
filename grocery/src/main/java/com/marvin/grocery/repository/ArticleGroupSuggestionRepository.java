package com.marvin.grocery.repository;

import com.marvin.grocery.entity.ArticleGroupSuggestionEntity;
import com.marvin.grocery.entity.SuggestionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ArticleGroupSuggestionEntity}. */
@Repository
public interface ArticleGroupSuggestionRepository extends JpaRepository<ArticleGroupSuggestionEntity, Long> {

    /**
     * Returns all suggestions with the given status, with their article and suggested group eagerly
     * fetched, ordered by score descending so the most confident suggestions surface first.
     *
     * @param status the status to match
     * @return list of matching suggestions with article and suggested group initialized
     */
    @Query("SELECT s FROM ArticleGroupSuggestionEntity s LEFT JOIN FETCH s.article LEFT JOIN FETCH s.suggestedGroup "
            + "WHERE s.status = :status ORDER BY s.score DESC")
    List<ArticleGroupSuggestionEntity> findAllWithDetailsByStatus(@Param("status") SuggestionStatus status);

    /**
     * Checks whether a suggestion with the given article id and status already exists.
     *
     * @param articleId the id of the article to check
     * @param status    the status to match
     * @return true if a matching suggestion exists
     */
    boolean existsByArticleIdAndStatus(Long articleId, SuggestionStatus status);
}
