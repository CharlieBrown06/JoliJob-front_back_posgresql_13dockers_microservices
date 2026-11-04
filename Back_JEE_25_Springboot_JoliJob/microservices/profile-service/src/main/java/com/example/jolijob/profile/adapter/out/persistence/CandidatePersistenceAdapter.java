package com.example.jolijob.profile.adapter.out.persistence;

import com.example.jolijob.profile.application.port.out.CandidateProfileRepository;
import com.example.jolijob.profile.domain.CandidateProfile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class CandidatePersistenceAdapter implements CandidateProfileRepository {
    private final CandidateJpaRepository repository;

    public CandidatePersistenceAdapter(CandidateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CandidateProfile> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(CandidateEntity::toDomain);
    }

    @Override
    public Optional<CandidateProfile> findById(UUID id) {
        return repository.findById(id).map(CandidateEntity::toDomain);
    }

    @Override
    @Transactional
    public CandidateProfile save(CandidateProfile profile) {
        return repository.saveAndFlush(CandidateEntity.from(profile)).toDomain();
    }
}
