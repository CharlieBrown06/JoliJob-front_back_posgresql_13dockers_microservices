package com.example.jolijob.profile.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CandidateJpaRepository extends JpaRepository<CandidateEntity, UUID> {
    Optional<CandidateEntity> findByUserId(UUID userId);
}
