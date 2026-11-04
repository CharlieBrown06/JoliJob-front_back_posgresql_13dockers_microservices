package com.example.jolijob.application.adapter.in.web;

import com.example.jolijob.application.adapter.out.kafka.ApplicationOutboxRelay;
import com.example.jolijob.application.adapter.out.persistence.SpringDataApplicationOutboxRepository;
import com.example.jolijob.application.domain.ApplicationEvent;
import com.example.jolijob.application.domain.ApplicationStatus;
import com.example.jolijob.application.port.out.CandidateProfilePort;
import com.example.jolijob.application.port.out.CompanyProfilePort;
import com.example.jolijob.application.port.out.JobOfferPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:application-controller-integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ApplicationControllerIntegrationTest {
    private static final UUID CANDIDATE_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID OTHER_CANDIDATE_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID COMPANY_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID OTHER_COMPANY_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID CANDIDATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID OTHER_CANDIDATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID COMPANY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID OTHER_COMPANY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000402");
    private static final UUID OFFER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000501");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SpringDataApplicationOutboxRepository outbox;

    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CandidateProfilePort candidates;
    @MockitoBean
    private CompanyProfilePort companies;
    @MockitoBean
    private JobOfferPort jobOffers;
    @MockitoBean
    private ApplicationOutboxRelay relay;
    @MockitoBean
    private KafkaTemplate<String, ApplicationEvent> kafkaTemplate;
    @MockitoBean
    private TaskScheduler taskScheduler;

    @BeforeEach
    void clearDatabaseAndPrepareExternalPorts() {
        outbox.deleteAll();
        jdbcTemplate.update("delete from applications");

        when(jwtDecoder.decode("candidate-token")).thenReturn(jwt(
                CANDIDATE_USER_ID, "candidate@example.test", "CANDIDATE"));
        when(jwtDecoder.decode("other-candidate-token")).thenReturn(jwt(
                OTHER_CANDIDATE_USER_ID, "other-candidate@example.test", "CANDIDATE"));
        when(jwtDecoder.decode("company-token")).thenReturn(jwt(
                COMPANY_USER_ID, "company@example.test", "COMPANY"));
        when(jwtDecoder.decode("other-company-token")).thenReturn(jwt(
                OTHER_COMPANY_USER_ID, "other-company@example.test", "COMPANY"));

        when(candidates.findByUserId(CANDIDATE_USER_ID))
                .thenReturn(Optional.of(new CandidateProfilePort.CandidateProfile(
                        CANDIDATE_ID, "https://files.example.test/candidate.pdf")));
        when(candidates.findByUserId(OTHER_CANDIDATE_USER_ID))
                .thenReturn(Optional.of(new CandidateProfilePort.CandidateProfile(
                        OTHER_CANDIDATE_ID, "https://files.example.test/other-candidate.pdf")));
        when(companies.findById(COMPANY_ID))
                .thenReturn(Optional.of(new CompanyProfilePort.CompanyProfile(
                        COMPANY_ID, "company@example.test")));
        when(companies.findByUserId(COMPANY_USER_ID))
                .thenReturn(Optional.of(new CompanyProfilePort.CompanyProfile(
                        COMPANY_ID, "company@example.test")));
        when(companies.findByUserId(OTHER_COMPANY_USER_ID))
                .thenReturn(Optional.of(new CompanyProfilePort.CompanyProfile(
                        OTHER_COMPANY_ID, "other-company@example.test")));
        when(jobOffers.findById(OFFER_ID))
                .thenReturn(Optional.of(new JobOfferPort.JobOffer(
                        OFFER_ID, true, COMPANY_ID, "Platform Engineer")));
    }

    @Test
    void appliesUpdatesAndWithdrawsWithOwnedActorsAndRejectsOtherActorsWithoutMutation() throws Exception {
        mvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer candidate-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jobOfferId":"%s","coverLetter":"I would love this role"}
                                """.formatted(OFFER_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(ApplicationStatus.SUBMITTED.name()))
                .andExpect(jsonPath("$.cvUrl").value("https://files.example.test/candidate.pdf"));

        UUID applicationId = UUID.fromString(jdbcTemplate.queryForObject(
                "select id from applications", String.class));
        assertEquals(1, outbox.count());
        assertEventTypes(List.of("SUBMITTED"));

        mvc.perform(patch("/api/v1/applications/{applicationId}/status", applicationId)
                        .header("Authorization", "Bearer company-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ApplicationStatus.ACCEPTED.name()));
        assertEquals(ApplicationStatus.ACCEPTED.name(), statusInDatabase(applicationId));
        assertEquals(2, outbox.count());
        assertEventTypes(List.of("SUBMITTED", "STATUS_CHANGED"));

        mvc.perform(patch("/api/v1/applications/{applicationId}/withdraw", applicationId)
                        .header("Authorization", "Bearer candidate-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ApplicationStatus.WITHDRAWN.name()));
        assertEquals(ApplicationStatus.WITHDRAWN.name(), statusInDatabase(applicationId));
        assertEquals(3, outbox.count());
        assertEventTypes(List.of("SUBMITTED", "STATUS_CHANGED", "WITHDRAWN"));

        mvc.perform(patch("/api/v1/applications/{applicationId}/status", applicationId)
                        .header("Authorization", "Bearer other-company-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"REJECTED"}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/applications/{applicationId}/withdraw", applicationId)
                        .header("Authorization", "Bearer other-candidate-token"))
                .andExpect(status().isForbidden());

        assertEquals(ApplicationStatus.WITHDRAWN.name(), statusInDatabase(applicationId));
        assertEquals(3, outbox.count());
        assertEventTypes(List.of("SUBMITTED", "STATUS_CHANGED", "WITHDRAWN"));
    }

    private String statusInDatabase(UUID applicationId) {
        return jdbcTemplate.queryForObject(
                "select status from applications where id = ?", String.class, applicationId);
    }

    private void assertEventTypes(List<String> expectedTypes) {
        List<String> payloads = jdbcTemplate.query(
                "select payload from application_outbox order by sequence_id",
                (resultSet, rowNumber) -> resultSet.getString("payload"));
        assertEquals(expectedTypes.size(), payloads.size());
        for (int index = 0; index < expectedTypes.size(); index++) {
            assertTrue(payloads.get(index).contains("\"type\":\"" + expectedTypes.get(index) + "\""),
                    "Expected outbox event type " + expectedTypes.get(index));
        }
    }

    private static Jwt jwt(UUID subject, String email, String role) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(role.toLowerCase() + "-token")
                .header("alg", "none")
                .subject(subject.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("email", email)
                .claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
    }
}
