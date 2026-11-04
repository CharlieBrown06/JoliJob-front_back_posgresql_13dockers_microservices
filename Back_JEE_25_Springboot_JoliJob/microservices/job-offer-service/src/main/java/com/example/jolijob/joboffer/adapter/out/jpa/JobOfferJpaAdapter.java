package com.example.jolijob.joboffer.adapter.out.jpa;

import com.example.jolijob.joboffer.application.port.out.JobOfferRepositoryPort;
import com.example.jolijob.joboffer.domain.JobOffer;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JobOfferJpaAdapter implements JobOfferRepositoryPort {
    private final SpringDataJobOfferRepository repository;

    public JobOfferJpaAdapter(SpringDataJobOfferRepository repository) {
        this.repository = repository;
    }

    @Override
    public JobOffer save(JobOffer offer) {
        return repository.saveAndFlush(JobOfferEntity.from(offer)).toDomain();
    }

    @Override
    public Optional<JobOffer> findById(UUID id) {
        return repository.findById(id).map(JobOfferEntity::toDomain);
    }

    @Override
    public void lockForUpdate(UUID id) {
        repository.findForUpdateById(id);
    }

    @Override
    public List<JobOffer> findActiveByCompanyId(UUID companyId) {
        return repository.findByCompanyIdAndActiveTrue(companyId).stream().map(JobOfferEntity::toDomain).toList();
    }
}
