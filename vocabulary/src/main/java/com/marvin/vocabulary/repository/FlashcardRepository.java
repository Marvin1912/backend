package com.marvin.vocabulary.repository;

import com.marvin.vocabulary.model.FlashcardEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashcardRepository extends JpaRepository<FlashcardEntity, Integer> {

    List<FlashcardEntity> findByAnkiIdIsNull();

    List<FlashcardEntity> findByUpdated(boolean updated);

}
