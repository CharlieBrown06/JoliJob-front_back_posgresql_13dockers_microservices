package com.example.jolijob.indexer.application.service;

import com.example.jolijob.indexer.application.domain.JobOfferEvent;
import com.example.jolijob.indexer.application.port.in.ProcessJobOfferEvent;
import com.example.jolijob.indexer.application.port.out.JobOfferIndexPort;

public class JobOfferEventHandler implements ProcessJobOfferEvent {

    private final JobOfferIndexPort jobOfferIndexPort;

    public JobOfferEventHandler(JobOfferIndexPort jobOfferIndexPort) {
        this.jobOfferIndexPort = jobOfferIndexPort;
    }

    @Override
    public void process(JobOfferEvent event) {
        switch (event.type()) {
            case CREATED -> jobOfferIndexPort.index(toIndexedOffer(event));
            case UPDATED -> jobOfferIndexPort.update(toUpdate(event));
            case DELETED -> jobOfferIndexPort.deactivate(event.jobOfferId());
        }
    }

    private JobOfferIndexPort.IndexedJobOffer toIndexedOffer(JobOfferEvent event) {
        return new JobOfferIndexPort.IndexedJobOffer(
                event.jobOfferId(),
                event.companyId(),
                event.companyName(),
                event.title(),
                event.description(),
                event.jobCategory(),
                event.specialty(),
                event.contractType(),
                event.remote(),
                event.country(),
                event.region(),
                event.department(),
                event.city(),
                event.salaryMin(),
                event.salaryMax(),
                true,
                event.occurredAt());
    }

    private JobOfferIndexPort.JobOfferUpdate toUpdate(JobOfferEvent event) {
        return new JobOfferIndexPort.JobOfferUpdate(
                event.jobOfferId(),
                event.title(),
                event.description(),
                event.jobCategory(),
                event.specialty(),
                event.contractType(),
                event.remote(),
                event.country(),
                event.region(),
                event.department(),
                event.city(),
                event.salaryMin(),
                event.salaryMax());
    }
}
