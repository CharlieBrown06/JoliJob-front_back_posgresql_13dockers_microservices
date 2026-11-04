package com.example.jolijob.profile.application;

import com.example.jolijob.profile.application.port.in.CompanyProfileUseCase;
import com.example.jolijob.profile.application.port.out.CompanyProfileRepository;
import com.example.jolijob.profile.domain.CompanyDetails;
import com.example.jolijob.profile.domain.CompanyProfile;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class CompanyProfiles implements CompanyProfileUseCase {
    private final CompanyProfileRepository repository;
    private final Clock clock;

    public CompanyProfiles(CompanyProfileRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public CompanyProfile create(UUID userId, CompanyDetails details) {
        if (repository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("Profil entreprise d\u00e9j\u00e0 existant");
        }
        Instant now = clock.instant();
        return repository.save(new CompanyProfile(UUID.randomUUID(), userId, details, now, now));
    }

    @Override
    public CompanyProfile update(UUID userId, CompanyDetails details) {
        return repository.save(getByUserId(userId).update(details, clock.instant()));
    }

    @Override
    public CompanyProfile getByUserId(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new OwnProfileNotFoundException("Profil entreprise introuvable"));
    }

    @Override
    public CompanyProfile getById(UUID companyId) {
        return repository.findById(companyId)
                .orElseThrow(() -> new ProfileNotFoundException("Entreprise introuvable : " + companyId));
    }
}
