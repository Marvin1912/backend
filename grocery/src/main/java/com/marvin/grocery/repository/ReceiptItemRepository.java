package com.marvin.grocery.repository;

import com.marvin.grocery.entity.ReceiptItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ReceiptItemEntity}. */
@Repository
public interface ReceiptItemRepository extends JpaRepository<ReceiptItemEntity, Long> {

    /**
     * Finds all items belonging to the given receipt.
     *
     * @param receiptId the UUID of the parent receipt
     * @return list of items for the specified receipt
     */
    List<ReceiptItemEntity> findByReceiptId(UUID receiptId);
}
