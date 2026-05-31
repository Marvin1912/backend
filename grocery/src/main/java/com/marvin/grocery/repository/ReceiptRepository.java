package com.marvin.grocery.repository;

import com.marvin.grocery.entity.ReceiptEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ReceiptEntity}. */
@Repository
public interface ReceiptRepository extends JpaRepository<ReceiptEntity, UUID> {
}
