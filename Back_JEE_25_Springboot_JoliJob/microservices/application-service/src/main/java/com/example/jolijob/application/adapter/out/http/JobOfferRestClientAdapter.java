package com.example.jolijob.application.adapter.out.http;

import com.example.jolijob.application.port.out.JobOfferPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

@Component
class JobOfferRestClientAdapter implements JobOfferPort {
    private final RestClient client;

    JobOfferRestClientAdapter(@Qualifier("jobOfferServiceRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public Optional<JobOffer> findById(UUID id) {
        return RestClientLookup.get(client, "/internal/job-offers/{id}", JobOfferResponse.class, id)
                .map(response -> new JobOffer(response.id(), response.active(), response.companyId(), response.title()));
    }

    private record JobOfferResponse(UUID id, boolean active, UUID companyId, String title) {
    }
}
