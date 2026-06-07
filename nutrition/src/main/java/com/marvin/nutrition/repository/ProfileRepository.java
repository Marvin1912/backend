package com.marvin.nutrition.repository;

import com.marvin.nutrition.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for the single-row nutrition profile. */
public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {
}
