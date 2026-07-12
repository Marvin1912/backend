package com.marvin.grocery.repository;

import com.marvin.grocery.entity.ArticleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
