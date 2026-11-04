package com.example.jolijob.joboffer.application.service;

import com.example.jolijob.joboffer.application.port.in.JobOfferUseCases;
import com.example.jolijob.joboffer.application.port.out.CompanyPort;
import com.example.jolijob.joboffer.application.port.out.JobOfferEventPort;
import com.example.jolijob.joboffer.application.port.out.JobOfferRepositoryPort;
import com.example.jolijob.joboffer.domain.JobOffer;
import com.example.jolijob.joboffer.domain.JobOfferDetails;
import com.example.jolijob.joboffer.domain.JobOfferEventType;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class JobOfferService implements JobOfferUseCases {
    private final JobOfferRepositoryPort repository;
    private final CompanyPort companies;
    private final JobOfferEventPort events;
    private final Clock clock;

    public JobOfferService(JobOfferRepositoryPort repository, CompanyPort companies,
                           JobOfferEventPort events, Clock clock) {
        this.repository = repository;
        this.companies = companies;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public JobOffer create(UUID userId, JobOfferDetails details) {
        var company = companies.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Cr\u00e9ez votre profil entreprise d'abord"));
        Instant now = clock.instant();
        JobOffer offer = repository.save(new JobOffer(UUID.randomUUID(), company.id(), company.name(),
                details, true, now, now));
        events.publish(offer, JobOfferEventType.CREATED);
        return offer;
    }

    @Override
    public JobOffer update(UUID userId, UUID offerId, JobOfferDetails details) {
        JobOffer offer = repository.save(getOwnedOffer(userId, offerId).update(details, clock.instant()));
        events.publish(offer, JobOfferEventType.UPDATED);
        return offer;
    }

    @Override
    public void delete(UUID userId, UUID offerId) {
        JobOffer offer = repository.save(getOwnedOffer(userId, offerId).deactivate(clock.instant()));
        events.publish(offer, JobOfferEventType.DELETED);
    }

    @Override
    public JobOffer getById(UUID offerId) {
        return repository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable : " + offerId));
    }

    @Override
    public List<JobOffer> getMyOffers(UUID userId) {
        return repository.findActiveByCompanyId(getCompany(userId).id());
    }

    private JobOffer getOwnedOffer(UUID userId, UUID offerId) {
        var company = getCompany(userId);
        JobOffer offer = getById(offerId);
        if (!offer.companyId().equals(company.id())) {
            throw new SecurityException("Acc\u00e8s refus\u00e9 \u00e0 cette offre");
        }
        return offer;
    }

    private CompanyPort.Company getCompany(UUID userId) {
        return companies.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Profil entreprise introuvable"));
    }
}
