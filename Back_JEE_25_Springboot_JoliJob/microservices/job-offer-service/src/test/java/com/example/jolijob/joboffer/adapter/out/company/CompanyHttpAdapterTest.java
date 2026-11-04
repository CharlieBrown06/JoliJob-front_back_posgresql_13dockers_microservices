package com.example.jolijob.joboffer.adapter.out.company;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

import static com.example.jolijob.joboffer.OfferFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CompanyHttpAdapterTest {
    private MockRestServiceServer server;
    private CompanyHttpAdapter adapter;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new CompanyHttpAdapter(builder, "http://company-service:8083");
        var jwt = Jwt.withTokenValue("caller-token").header("alg", "RS256")
                .subject(USER_ID.toString()).claim("email", "company@example.com")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("COMPANY"))).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        server.verify();
    }

    @Test
    void forwardsBearerTokenAndReadsExistingCompanyProfileShape() {
        server.expect(requestTo("http://company-service:8083/api/v1/companies/profile/me"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer caller-token"))
                .andRespond(withSuccess(profile(USER_ID), MediaType.APPLICATION_JSON));
        var company = adapter.findByUserId(USER_ID).orElseThrow();
        assertThat(company.id()).isEqualTo(COMPANY_ID);
        assertThat(company.name()).isEqualTo("Joli Company");
        assertThat(company.userId()).isEqualTo(USER_ID);
    }

    @Test
    void returnsEmptyForMissingProfile() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.CONFLICT));
        assertThat(adapter.findByUserId(USER_ID)).isEmpty();
    }

    @Test
    void rejectsMismatchedCompanyOwner() {
        server.expect(anything()).andRespond(withSuccess(profile(UUID.randomUUID()), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.findByUserId(USER_ID)).isInstanceOf(SecurityException.class);
    }

    @Test
    void propagatesUpstreamFailureRatherThanTreatingItAsMissingProfile() {
        server.expect(anything()).andRespond(withServerError());
        assertThatThrownBy(() -> adapter.findByUserId(USER_ID)).isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void refusesLookupWithoutCallerIdentity() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> adapter.findByUserId(USER_ID)).isInstanceOf(SecurityException.class);
    }

    @Test
    void doesNotTreatAnEmptySuccessfulResponseAsAMissingProfile() {
        server.expect(anything()).andRespond(withSuccess());
        assertThatThrownBy(() -> adapter.findByUserId(USER_ID))
                .isInstanceOf(RestClientException.class).hasMessage("Company service returned an empty profile");
    }

    private static String profile(UUID userId) {
        return """
                {"id":"%s","userId":"%s","name":"Joli Company","siret":"123",
                 "description":"Company profile","website":"https://example.com","sector":"IT",
                 "country":"France","city":"Paris","logoUrl":null,"contactEmail":"hr@example.com",
                 "contactPhone":null,"createdAt":"2026-10-20T12:00:00Z","updatedAt":"2026-10-20T12:00:00Z"}
                """.formatted(COMPANY_ID, userId);
    }
}
