package com.example.jolijob.joboffer.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataJobOfferRepository extends JpaRepository<JobOfferEntity, UUID> {
    List<JobOfferEntity> findByCompanyIdAndActiveTrue(UUID companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select offer from JobOfferEntity offer where offer.id = :id")
    Optional<JobOfferEntity> findForUpdateById(@Param("id") UUID id);
}
