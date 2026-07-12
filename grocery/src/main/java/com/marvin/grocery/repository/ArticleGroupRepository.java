package com.marvin.grocery.repository;

import com.marvin.grocery.entity.ArticleGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ArticleGroupEntity}. */
@Repository
public interface ArticleGroupRepository extends JpaRepository<ArticleGroupEntity, Long> {
}
