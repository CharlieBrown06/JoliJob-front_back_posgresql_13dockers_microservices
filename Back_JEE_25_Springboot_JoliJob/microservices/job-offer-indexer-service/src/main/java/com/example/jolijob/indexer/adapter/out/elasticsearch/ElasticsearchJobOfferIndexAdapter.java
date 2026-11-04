package com.example.jolijob.indexer.adapter.out.elasticsearch;

import com.example.jolijob.indexer.application.port.out.JobOfferIndexPort;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchJobOfferIndexAdapter implements JobOfferIndexPort {

    private final JobOfferDocumentRepository repository;

    public ElasticsearchJobOfferIndexAdapter(JobOfferDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public void index(IndexedJobOffer offer) {
        repository.save(new JobOfferDocument(
                offer.id(),
                offer.companyId(),
                offer.companyName(),
                offer.title(),
                offer.description(),
                offer.jobCategory(),
                offer.specialty(),
                offer.contractType(),
                offer.remote(),
                offer.country(),
                offer.region(),
                offer.department(),
                offer.city(),
                offer.salaryMin(),
                offer.salaryMax(),
                offer.active(),
                offer.createdAt()));
    }

    @Override
    public void update(JobOfferUpdate offer) {
        repository.findById(offer.id()).ifPresent(existing -> repository.save(new JobOfferDocument(
                existing.id(),
                existing.companyId(),
                existing.companyName(),
                offer.title(),
                offer.description(),
                offer.jobCategory(),
                offer.specialty(),
                offer.contractType(),
                offer.remote(),
                offer.country(),
                offer.region(),
                offer.department(),
                offer.city(),
                offer.salaryMin(),
                offer.salaryMax(),
                existing.active(),
                existing.createdAt())));
    }

    @Override
    public void deactivate(String jobOfferId) {
        repository.findById(jobOfferId).ifPresent(existing -> repository.save(new JobOfferDocument(
                existing.id(),
                existing.companyId(),
                existing.companyName(),
                existing.title(),
                existing.description(),
                existing.jobCategory(),
                existing.specialty(),
                existing.contractType(),
                existing.remote(),
                existing.country(),
                existing.region(),
                existing.department(),
                existing.city(),
                existing.salaryMin(),
                existing.salaryMax(),
                false,
                existing.createdAt())));
    }

}
