package com.example.jolijob.profile.adapter.out.persistence;

import com.example.jolijob.profile.application.port.out.CompanyProfileRepository;
import com.example.jolijob.profile.domain.CompanyProfile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class CompanyPersistenceAdapter implements CompanyProfileRepository {
    private final CompanyJpaRepository repository;

    public CompanyPersistenceAdapter(CompanyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CompanyProfile> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(CompanyEntity::toDomain);
    }

    @Override
    public Optional<CompanyProfile> findById(UUID id) {
        return repository.findById(id).map(CompanyEntity::toDomain);
    }

    @Override
    @Transactional
    public CompanyProfile save(CompanyProfile profile) {
        return repository.saveAndFlush(CompanyEntity.from(profile)).toDomain();
    }
}
