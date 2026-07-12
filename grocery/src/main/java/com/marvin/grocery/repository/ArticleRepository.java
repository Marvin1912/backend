package com.marvin.grocery.repository;

import com.marvin.grocery.entity.ArticleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ArticleEntity}. */
@Repository
public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {

    /**
     * Finds the article matching the given normalized (lower-cased, trimmed) name.
     *
     * @param normalizedName the normalized product name to match
     * @return an Optional containing the article if one exists for that normalized name
     */
    Optional<ArticleEntity> findByNormalizedName(String normalizedName);

    /**
     * Clears the group assignment for every article referencing the given group id, so deleting a
     * group only decouples its articles instead of cascading the delete onto them.
     *
     * @param groupId the id of the article group being detached
     */
    @Modifying
    @Query("UPDATE ArticleEntity a SET a.articleGroup = null WHERE a.articleGroup.id = :groupId")
    void clearArticleGroup(@Param("groupId") Long groupId);
}
