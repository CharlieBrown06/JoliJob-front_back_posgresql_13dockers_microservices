package com.example.jolijob.application.adapter.out.http;

import com.example.jolijob.application.port.out.CandidateProfilePort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

@Component
class CandidateProfileRestClientAdapter implements CandidateProfilePort {
    private final RestClient client;

    CandidateProfileRestClientAdapter(@Qualifier("profileServiceRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public Optional<CandidateProfile> findByUserId(UUID userId) {
        return RestClientLookup.get(client, "/internal/profiles/candidates/by-user/{userId}",
                        CandidateProfileResponse.class, userId)
                .map(response -> new CandidateProfile(response.id(), response.cvUrl()));
    }

    private record CandidateProfileResponse(UUID id, String cvUrl) {
    }
}
