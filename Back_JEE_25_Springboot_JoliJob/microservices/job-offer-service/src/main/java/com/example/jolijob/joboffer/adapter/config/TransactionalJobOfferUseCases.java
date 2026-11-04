package com.example.jolijob.joboffer.adapter.config;

import com.example.jolijob.joboffer.application.port.in.JobOfferUseCases;
import com.example.jolijob.joboffer.application.port.out.JobOfferRepositoryPort;
import com.example.jolijob.joboffer.application.service.JobOfferService;
import com.example.jolijob.joboffer.domain.JobOffer;
import com.example.jolijob.joboffer.domain.JobOfferDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class TransactionalJobOfferUseCases implements JobOfferUseCases {
    private final JobOfferService delegate;
    private final JobOfferRepositoryPort repository;

    public TransactionalJobOfferUseCases(JobOfferService delegate, JobOfferRepositoryPort repository) {
        this.delegate = delegate;
        this.repository = repository;
    }

    @Override
    @Transactional
    public JobOffer create(UUID userId, JobOfferDetails details) {
        return delegate.create(userId, details);
    }

    @Override
    @Transactional
    public JobOffer update(UUID userId, UUID offerId, JobOfferDetails details) {
        repository.lockForUpdate(offerId);
        return delegate.update(userId, offerId, details);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID offerId) {
        repository.lockForUpdate(offerId);
        delegate.delete(userId, offerId);
    }

    @Override
    public JobOffer getById(UUID offerId) {
        return delegate.getById(offerId);
    }

    @Override
    public List<JobOffer> getMyOffers(UUID userId) {
        return delegate.getMyOffers(userId);
    }
}
