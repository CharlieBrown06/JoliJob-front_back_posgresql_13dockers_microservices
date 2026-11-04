package com.example.jolijob.application.adapter.out.persistence;

import com.example.jolijob.application.domain.Application;
import com.example.jolijob.application.port.out.ApplicationRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class ApplicationPersistenceAdapter implements ApplicationRepositoryPort {
    private final SpringDataApplicationRepository repository;

    ApplicationPersistenceAdapter(SpringDataApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Application save(Application application) {
        return repository.save(ApplicationEntity.from(application)).toDomain();
    }

    @Override
    public Optional<Application> findById(UUID id) {
        return repository.findById(id).map(ApplicationEntity::toDomain);
    }

    @Override
    public void lockById(UUID id) {
        repository.findByIdForUpdate(id);
    }

    @Override
    public boolean existsByJobOfferIdAndCandidateId(UUID jobOfferId, UUID candidateId) {
        return repository.existsByJobOfferIdAndCandidateId(jobOfferId, candidateId);
    }

    @Override
    public List<Application> findByCandidateId(UUID candidateId) {
        return repository.findByCandidateId(candidateId).stream().map(ApplicationEntity::toDomain).toList();
    }

    @Override
    public List<Application> findByJobOfferId(UUID jobOfferId) {
        return repository.findByJobOfferId(jobOfferId).stream().map(ApplicationEntity::toDomain).toList();
    }
}
