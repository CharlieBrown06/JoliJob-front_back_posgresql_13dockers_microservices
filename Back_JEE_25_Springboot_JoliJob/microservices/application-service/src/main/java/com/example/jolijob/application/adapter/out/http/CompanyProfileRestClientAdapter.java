package com.example.jolijob.application.adapter.out.http;

import com.example.jolijob.application.port.out.CompanyProfilePort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

@Component
class CompanyProfileRestClientAdapter implements CompanyProfilePort {
    private final RestClient client;

    CompanyProfileRestClientAdapter(@Qualifier("profileServiceRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public Optional<CompanyProfile> findByUserId(UUID userId) {
        return RestClientLookup.get(client, "/internal/profiles/companies/by-user/{userId}",
                        CompanyProfileResponse.class, userId)
                .map(response -> new CompanyProfile(response.id(), response.contactEmail()));
    }

    @Override
    public Optional<CompanyProfile> findById(UUID companyId) {
        return RestClientLookup.get(client, "/internal/profiles/companies/{companyId}",
                        CompanyProfileResponse.class, companyId)
                .map(response -> new CompanyProfile(response.id(), response.contactEmail()));
    }

    private record CompanyProfileResponse(UUID id, String contactEmail) {
    }
}
