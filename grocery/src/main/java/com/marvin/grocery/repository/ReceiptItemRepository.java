package com.marvin.grocery.repository;

import com.marvin.grocery.entity.ReceiptItemEntity;
import java.util.List;
import java.util.Optional;
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

    /**
     * Finds a single item by its id that belongs to the given receipt.
     *
     * @param id        the item id
     * @param receiptId the UUID of the parent receipt
     * @return an Optional containing the item if it exists and belongs to the receipt
     */
    Optional<ReceiptItemEntity> findByIdAndReceiptId(Long id, UUID receiptId);
}
