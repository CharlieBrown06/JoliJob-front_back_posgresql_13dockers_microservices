package com.example.jolijob.profile.application;

import com.example.jolijob.profile.application.port.in.CandidateProfileUseCase;
import com.example.jolijob.profile.application.port.out.CandidateProfileRepository;
import com.example.jolijob.profile.domain.CandidateDetails;
import com.example.jolijob.profile.domain.CandidateProfile;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class CandidateProfiles implements CandidateProfileUseCase {
    private final CandidateProfileRepository repository;
    private final Clock clock;

    public CandidateProfiles(CandidateProfileRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public CandidateProfile create(UUID userId, CandidateDetails details) {
        if (repository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("Profil candidat d\u00e9j\u00e0 existant");
        }
        Instant now = clock.instant();
        return repository.save(new CandidateProfile(UUID.randomUUID(), userId, details, now, now));
    }

    @Override
    public CandidateProfile update(UUID userId, CandidateDetails details) {
        return repository.save(getByUserId(userId).update(details, clock.instant()));
    }

    @Override
    public CandidateProfile getByUserId(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new OwnProfileNotFoundException("Profil candidat introuvable"));
    }

    @Override
    public CandidateProfile getById(UUID candidateId) {
        return repository.findById(candidateId)
                .orElseThrow(() -> new ProfileNotFoundException("Candidat introuvable : " + candidateId));
    }
}
