package com.example.jolijob.joboffer.adapter.in.rest;

import com.example.jolijob.joboffer.adapter.out.outbox.JobOfferOutboxRelay;
import com.example.jolijob.joboffer.application.port.out.CompanyPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.example.jolijob.joboffer.OfferFixture.COMPANY_ID;
import static com.example.jolijob.joboffer.OfferFixture.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:job-offer-persistence;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "job-offer.outbox.relay.enabled=false"
})
@AutoConfigureMockMvc
class JobOfferPersistenceIntegrationTest {
    private static final UUID OTHER_COMPANY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final String OFFER_BODY = """
            {"title":"Java developer","description":"Build services","jobCategory":"IT",
             "specialty":"Backend","contractType":"CDI","remote":true,"country":"France",
             "region":"Ile-de-France","department":"75","city":"Paris","salaryMin":50000,
             "salaryMax":65000,"startDate":"2026-11-01","expiresAt":"2026-12-01"}
            """;
    private static final String UPDATED_OFFER_BODY = """
            {"title":"Senior Java developer","description":"Lead service development",
             "jobCategory":"IT","specialty":"Backend","contractType":"CDI","remote":false,
             "country":"France","region":"Ile-de-France","department":"75","city":"Paris",
             "salaryMin":60000,"salaryMax":75000,"startDate":"2026-11-01","expiresAt":"2026-12-01"}
            """;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;

    @MockitoBean CompanyPort companies;
    @MockitoBean KafkaTemplate<String, Object> kafka;
    @MockitoBean JwtDecoder decoder;

    @BeforeEach
    void clearDatabaseAndConfigureExternalAdapters() {
        jdbc.update("DELETE FROM job_offer_outbox");
        jdbc.update("DELETE FROM job_offers");
        when(companies.findByUserId(USER_ID)).thenReturn(Optional.of(
                new CompanyPort.Company(COMPANY_ID, USER_ID, "Joli Company")));
        when(companies.findByUserId(OTHER_COMPANY_ID)).thenReturn(Optional.of(
                new CompanyPort.Company(OTHER_COMPANY_ID, OTHER_COMPANY_ID, "Other Company")));
        when(kafka.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(decoder.decode("company")).thenReturn(token("COMPANY", USER_ID));
        when(decoder.decode("other-company")).thenReturn(token("COMPANY", OTHER_COMPANY_ID));
        when(decoder.decode("candidate")).thenReturn(token("CANDIDATE", USER_ID));
    }

    @Test
    void externalRequestsPersistOfferLifecycleAndMatchingOutboxEvents() throws Exception {
        var createdResponse = mvc.perform(post("/api/v1/job-offers")
                        .header("Authorization", "Bearer company")
                        .contentType(APPLICATION_JSON).content(OFFER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyId").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$.companyName").value("Joli Company"))
                .andExpect(jsonPath("$.title").value("Java developer"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        UUID offerId = UUID.fromString(json.readTree(createdResponse.getResponse().getContentAsString())
                .get("id").asString());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_offers WHERE id = ?", Integer.class, offerId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM job_offer_outbox
                WHERE aggregate_id = ? AND topic = ?
                """, Integer.class, offerId, JobOfferOutboxRelay.TOPIC)).isEqualTo(1);
        assertThat(outboxTypes(offerId)).containsExactly("CREATED");

        mvc.perform(get("/api/v1/job-offers/mine").header("Authorization", "Bearer company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(offerId.toString()))
                .andExpect(jsonPath("$[0].title").value("Java developer"));
        mvc.perform(get("/api/v1/job-offers/{id}", offerId).header("Authorization", "Bearer candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(offerId.toString()))
                .andExpect(jsonPath("$.title").value("Java developer"));

        mvc.perform(put("/api/v1/job-offers/{id}", offerId).header("Authorization", "Bearer company")
                        .contentType(APPLICATION_JSON).content(UPDATED_OFFER_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Senior Java developer"))
                .andExpect(jsonPath("$.remote").value(false));
        assertThat(jdbc.queryForObject("SELECT title FROM job_offers WHERE id = ?", String.class, offerId))
                .isEqualTo("Senior Java developer");
        assertThat(outboxTypes(offerId)).containsExactly("CREATED", "UPDATED");

        mvc.perform(delete("/api/v1/job-offers/{id}", offerId)
                        .header("Authorization", "Bearer other-company"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT active FROM job_offers WHERE id = ?", Boolean.class, offerId))
                .isTrue();
        assertThat(outboxTypes(offerId)).containsExactly("CREATED", "UPDATED");

        mvc.perform(delete("/api/v1/job-offers/{id}", offerId).header("Authorization", "Bearer company"))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT active FROM job_offers WHERE id = ?", Boolean.class, offerId))
                .isFalse();
        assertThat(outboxTypes(offerId)).containsExactly("CREATED", "UPDATED", "DELETED");
        mvc.perform(get("/internal/job-offers/{id}", offerId).header("Authorization", "Bearer candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(offerId.toString()))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.companyId").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$.title").value("Senior Java developer"));
    }

    @Test
    void rejectsInvalidRequestsAndWrongRoleWithoutPersistingOffersOrEvents() throws Exception {
        mvc.perform(post("/api/v1/job-offers").header("Authorization", "Bearer company")
                        .contentType(APPLICATION_JSON).content("""
                                {"title":" ","description":"","contractType":null}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/job-offers").header("Authorization", "Bearer candidate")
                        .contentType(APPLICATION_JSON).content(OFFER_BODY))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/job-offers").contentType(APPLICATION_JSON).content(OFFER_BODY))
                .andExpect(status().isUnauthorized());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_offers", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_offer_outbox", Integer.class)).isZero();
    }

    private List<String> outboxTypes(UUID offerId) {
        return jdbc.queryForList("""
                SELECT payload FROM job_offer_outbox WHERE aggregate_id = ? ORDER BY event_order
                """, String.class, offerId).stream()
                .map(payload -> json.readTree(payload).get("type").asString())
                .toList();
    }

    private static Jwt token(String role, UUID subject) {
        return Jwt.withTokenValue("test-token").header("alg", "RS256")
                .subject(subject.toString())
                .issuer("http://localhost:8081/realms/jolijob")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .claim("aud", List.of("jolijob-api"))
                .claim("email", "company@example.com").claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
    }
}
