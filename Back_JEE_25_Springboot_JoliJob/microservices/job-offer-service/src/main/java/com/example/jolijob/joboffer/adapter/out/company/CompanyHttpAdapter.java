package com.example.jolijob.joboffer.adapter.out.company;

import com.example.jolijob.joboffer.application.port.out.CompanyPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

@Component
public class CompanyHttpAdapter implements CompanyPort {
    private final RestClient client;

    public CompanyHttpAdapter(RestClient.Builder builder, @Value("${company.base-url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Optional<Company> findByUserId(UUID userId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new SecurityException("Company lookup requires a bearer token");
        }
        // Forward the caller's identity to the existing company profile API; never trust a client-supplied company ID.
        Company company = client.get().uri("/api/v1/companies/profile/me")
                .headers(headers -> headers.setBearerAuth(token.getToken().getTokenValue()))
                .exchange((request, response) -> {
                    if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)
                            || response.getStatusCode().equals(HttpStatus.CONFLICT)) {
                        return null;
                    }
                    if (response.getStatusCode().isError()) {
                        throw response.createException();
                    }
                    Company profile = response.bodyTo(Company.class);
                    if (profile == null) {
                        throw new RestClientException("Company service returned an empty profile");
                    }
                    return profile;
                });
        if (company == null) {
            return Optional.empty();
        }
        if (company.id() == null || !userId.equals(company.userId())) {
            throw new SecurityException("Company profile does not belong to the current user");
        }
        return Optional.of(company);
    }
}
