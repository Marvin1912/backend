package com.marvin.grocery.repository;

import com.marvin.grocery.entity.ReceiptEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ReceiptEntity}. */
@Repository
public interface ReceiptRepository extends JpaRepository<ReceiptEntity, UUID> {

    /**
     * Returns all receipts with their items eagerly fetched to avoid lazy-load issues.
     *
     * @return list of all receipt entities with items initialized
     */
    @Query("SELECT DISTINCT r FROM ReceiptEntity r LEFT JOIN FETCH r.items")
    List<ReceiptEntity> findAllWithItems();
}
