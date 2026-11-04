package com.example.jolijob.application.adapter.out.http;

import com.example.jolijob.application.port.out.CandidateProfilePort;
import com.example.jolijob.application.port.out.CompanyProfilePort;
import com.example.jolijob.application.port.out.JobOfferPort;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestClientAdaptersTest {
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    void mapsCandidateProfileResponseAndReturnsNotFoundAsEmpty() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://profile-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CandidateProfileRestClientAdapter adapter = new CandidateProfileRestClientAdapter(builder.build());
        server.expect(requestTo("http://profile-service/internal/profiles/candidates/by-user/" + USER_ID))
                .andRespond(withSuccess("""
                        {"id":"%s","cvUrl":"https://files.example.test/candidate.pdf"}
                        """.formatted(PROFILE_ID), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://profile-service/internal/profiles/candidates/by-user/" + USER_ID))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<CandidateProfilePort.CandidateProfile> found = adapter.findByUserId(USER_ID);

        assertEquals(Optional.of(new CandidateProfilePort.CandidateProfile(
                PROFILE_ID, "https://files.example.test/candidate.pdf")), found);
        assertTrue(adapter.findByUserId(USER_ID).isEmpty());
        server.verify();
    }

    @Test
    void mapsCompanyProfileByUserAndById() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://profile-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CompanyProfileRestClientAdapter adapter = new CompanyProfileRestClientAdapter(builder.build());
        String response = """
                {"id":"%s","contactEmail":"company@example.test"}
                """.formatted(PROFILE_ID);
        server.expect(requestTo("http://profile-service/internal/profiles/companies/by-user/" + USER_ID))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://profile-service/internal/profiles/companies/" + COMPANY_ID))
                .andRespond(withSuccess("""
                        {"id":"%s","contactEmail":"company@example.test"}
                        """.formatted(COMPANY_ID), MediaType.APPLICATION_JSON));

        assertEquals(Optional.of(new CompanyProfilePort.CompanyProfile(PROFILE_ID, "company@example.test")),
                adapter.findByUserId(USER_ID));
        assertEquals(Optional.of(new CompanyProfilePort.CompanyProfile(COMPANY_ID, "company@example.test")),
                adapter.findById(COMPANY_ID));
        server.verify();
    }

    @Test
    void mapsJobOfferResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://job-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        JobOfferRestClientAdapter adapter = new JobOfferRestClientAdapter(builder.build());
        server.expect(requestTo("http://job-service/internal/job-offers/" + OFFER_ID))
                .andRespond(withSuccess("""
                        {"id":"%s","active":true,"companyId":"%s","title":"Backend Engineer"}
                        """.formatted(OFFER_ID, COMPANY_ID), MediaType.APPLICATION_JSON));

        assertEquals(Optional.of(new JobOfferPort.JobOffer(OFFER_ID, true, COMPANY_ID, "Backend Engineer")),
                adapter.findById(OFFER_ID));
        server.verify();
    }

    @Test
    void propagatesServerErrorsInsteadOfTreatingThemAsMissingResources() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://job-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        JobOfferRestClientAdapter adapter = new JobOfferRestClientAdapter(builder.build());
        server.expect(requestTo("http://job-service/internal/job-offers/" + OFFER_ID))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        RestClientResponseException exception = assertThrows(RestClientResponseException.class,
                () -> adapter.findById(OFFER_ID));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        server.verify();
    }
}
