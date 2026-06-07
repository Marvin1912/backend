package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.FoodEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link FoodEntity}. */
@Repository
public interface FoodRepository extends JpaRepository<FoodEntity, UUID> {

    /**
     * Searches for food entries whose name contains the given query string, case-insensitively,
     * ordered alphabetically by name.
     *
     * @param q the substring to search for within food names
     * @return list of matching food entities ordered by name
     */
    @Query("SELECT f FROM FoodEntity f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY f.name")
    List<FoodEntity> searchByName(String q);
}
