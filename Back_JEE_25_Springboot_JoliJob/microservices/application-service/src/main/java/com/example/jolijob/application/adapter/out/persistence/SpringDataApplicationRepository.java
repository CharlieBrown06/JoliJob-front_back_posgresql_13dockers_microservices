package com.example.jolijob.application.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from ApplicationEntity application where application.id = :id")
    Optional<ApplicationEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByJobOfferIdAndCandidateId(UUID jobOfferId, UUID candidateId);

    List<ApplicationEntity> findByCandidateId(UUID candidateId);

    List<ApplicationEntity> findByJobOfferId(UUID jobOfferId);
}
