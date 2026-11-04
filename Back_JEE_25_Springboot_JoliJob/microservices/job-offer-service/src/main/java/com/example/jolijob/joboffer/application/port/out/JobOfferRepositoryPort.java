package com.example.jolijob.joboffer.application.port.out;

import com.example.jolijob.joboffer.domain.JobOffer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobOfferRepositoryPort {
    JobOffer save(JobOffer offer);
    Optional<JobOffer> findById(UUID id);
    void lockForUpdate(UUID id);
    List<JobOffer> findActiveByCompanyId(UUID companyId);
}
