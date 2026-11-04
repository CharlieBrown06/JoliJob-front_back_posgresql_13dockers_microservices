package com.example.jolijob.joboffer.repository;

import com.example.jolijob.joboffer.domain.JobOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobOfferRepository extends JpaRepository<JobOffer, UUID> {
    List<JobOffer> findByCompanyIdAndActiveTrue(UUID companyId);
    List<JobOffer> findByActiveTrue();
}
