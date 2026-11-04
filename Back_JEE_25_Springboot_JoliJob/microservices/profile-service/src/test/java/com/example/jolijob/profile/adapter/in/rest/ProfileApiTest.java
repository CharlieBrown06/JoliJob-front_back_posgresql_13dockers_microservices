package com.example.jolijob.profile.adapter.in.rest;

import com.example.jolijob.profile.adapter.out.persistence.CandidateJpaRepository;
import com.example.jolijob.profile.adapter.out.persistence.CompanyJpaRepository;
import com.example.jolijob.profile.application.port.in.CandidateProfileUseCase;
import com.example.jolijob.profile.application.port.in.CompanyProfileUseCase;
import com.example.jolijob.profile.application.port.out.CandidateProfileRepository;
import com.example.jolijob.profile.application.port.out.CompanyProfileRepository;
import com.example.jolijob.profile.domain.CandidateDetails;
import com.example.jolijob.profile.domain.CandidateProfile;
import com.example.jolijob.profile.domain.CompanyDetails;
import com.example.jolijob.profile.domain.CompanyProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profile-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Import(ProfileApiTest.Tokens.class)
class ProfileApiTest {
    private static final UUID CANDIDATE_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID COMPANY_USER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_CANDIDATE_USER = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID OTHER_COMPANY_USER = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final String CANDIDATE_BODY = """
            {"firstName":"Ada","lastName":"Lovelace","phone":"123","city":"London","country":"UK",
             "jobTitle":"Engineer","summary":"Summary","cvUrl":"https://example.org/cv.pdf",
             "linkedinUrl":"linkedin","portfolioUrl":"portfolio","birthDate":"1990-01-01",
             "openToRemote":true,"openToRelocation":false}
            """;
    private static final String COMPANY_BODY = """
            {"name":"Acme","siret":"12345678901234","description":"Description",
             "website":"https://acme.org","sector":"Technology","country":"France","city":"Paris",
             "logoUrl":"logo","contactEmail":"jobs@acme.org","contactPhone":"456"}
            """;

    @Autowired MockMvc mvc;
    @Autowired CandidateProfileUseCase candidates;
    @Autowired CompanyProfileUseCase companies;
    @Autowired CandidateJpaRepository candidateRows;
    @Autowired CompanyJpaRepository companyRows;
    @Autowired CandidateProfileRepository candidateRepository;
    @Autowired CompanyProfileRepository companyRepository;

    @BeforeEach
    void clearProfiles() {
        candidateRows.deleteAll();
        companyRows.deleteAll();
    }

    @Test
    void candidateLifecyclePreservesContractAndPersistsAllFields() throws Exception {
        mvc.perform(post("/api/v1/candidates/profile").header("Authorization", "Bearer candidate")
                        .contentType(MediaType.APPLICATION_JSON).content(CANDIDATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", aMapWithSize(16)))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(CANDIDATE_USER.toString()))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.phone").value("123"))
                .andExpect(jsonPath("$.city").value("London"))
                .andExpect(jsonPath("$.country").value("UK"))
                .andExpect(jsonPath("$.jobTitle").value("Engineer"))
                .andExpect(jsonPath("$.summary").value("Summary"))
                .andExpect(jsonPath("$.cvUrl").value("https://example.org/cv.pdf"))
                .andExpect(jsonPath("$.linkedinUrl").value("linkedin"))
                .andExpect(jsonPath("$.portfolioUrl").value("portfolio"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
                .andExpect(jsonPath("$.openToRemote").value(true))
                .andExpect(jsonPath("$.openToRelocation").value(false))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
        CandidateProfile original = candidates.getByUserId(CANDIDATE_USER);
        assertThat(original.details()).isEqualTo(new CandidateDetails("Ada", "Lovelace", "123",
                "London", "UK", "Engineer", "Summary", "https://example.org/cv.pdf", "linkedin",
                "portfolio", LocalDate.of(1990, 1, 1), true, false));

        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization", "Bearer candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id().toString()))
                .andExpect(jsonPath("$.cvUrl").value("https://example.org/cv.pdf"));
        mvc.perform(get("/api/v1/candidates/{id}", original.id()).header("Authorization", "Bearer company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id().toString()));
        mvc.perform(put("/api/v1/candidates/profile").header("Authorization", "Bearer candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Grace","lastName":"Hopper","openToRelocation":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id().toString()))
                .andExpect(jsonPath("$.firstName").value("Grace"))
                .andExpect(jsonPath("$.openToRemote").value(false))
                .andExpect(jsonPath("$.openToRelocation").value(true))
                .andExpect(jsonPath("$.cvUrl").isEmpty());

        CandidateProfile updated = candidates.getById(original.id());
        assertThat(updated.createdAt()).isEqualTo(original.createdAt());
        assertThat(updated.updatedAt()).isAfterOrEqualTo(original.updatedAt());
        assertThat(updated.details().firstName()).isEqualTo("Grace");
        assertThat(updated.details().phone()).isNull();
        assertThat(candidateRows.count()).isEqualTo(1);
    }

    @Test
    void companyLifecyclePreservesContractAndPersistsAllFields() throws Exception {
        mvc.perform(post("/api/v1/companies/profile").header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content(COMPANY_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", aMapWithSize(14)))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(COMPANY_USER.toString()))
                .andExpect(jsonPath("$.name").value("Acme"))
                .andExpect(jsonPath("$.siret").value("12345678901234"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.website").value("https://acme.org"))
                .andExpect(jsonPath("$.sector").value("Technology"))
                .andExpect(jsonPath("$.country").value("France"))
                .andExpect(jsonPath("$.city").value("Paris"))
                .andExpect(jsonPath("$.logoUrl").value("logo"))
                .andExpect(jsonPath("$.contactEmail").value("jobs@acme.org"))
                .andExpect(jsonPath("$.contactPhone").value("456"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        CompanyProfile original = companies.getByUserId(COMPANY_USER);
        assertThat(original.details()).isEqualTo(new CompanyDetails("Acme", "12345678901234",
                "Description", "https://acme.org", "Technology", "France", "Paris", "logo",
                "jobs@acme.org", "456"));

        mvc.perform(get("/api/v1/companies/profile/me").header("Authorization", "Bearer company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id().toString()))
                .andExpect(jsonPath("$.contactEmail").value("jobs@acme.org"));
        mvc.perform(get("/api/v1/companies/{id}", original.id()).header("Authorization", "Bearer candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id().toString()));
        mvc.perform(put("/api/v1/companies/profile").header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"New Acme","contactEmail":"new@acme.org"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id().toString()))
                .andExpect(jsonPath("$.name").value("New Acme"))
                .andExpect(jsonPath("$.contactEmail").value("new@acme.org"));

        CompanyProfile updated = companies.getById(original.id());
        assertThat(updated.createdAt()).isEqualTo(original.createdAt());
        assertThat(updated.updatedAt()).isAfterOrEqualTo(original.updatedAt());
        assertThat(updated.details().contactEmail()).isEqualTo("new@acme.org");
        assertThat(updated.details().siret()).isNull();
        assertThat(companyRows.count()).isEqualTo(1);
    }

    @Test
    void authenticatedCandidateAndCompanyProfilesRemainIsolatedByOwner() throws Exception {
        mvc.perform(post("/api/v1/candidates/profile").header("Authorization", "Bearer candidate")
                        .contentType(MediaType.APPLICATION_JSON).content(CANDIDATE_BODY))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/candidates/profile").header("Authorization", "Bearer other-candidate")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"firstName":"Grace","lastName":"Hopper"}
                                """))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/v1/candidates/profile").header("Authorization", "Bearer other-candidate")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"firstName":"Grace Updated","lastName":"Hopper"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization", "Bearer candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(CANDIDATE_USER.toString()))
                .andExpect(jsonPath("$.firstName").value("Ada"));
        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization", "Bearer other-candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(OTHER_CANDIDATE_USER.toString()))
                .andExpect(jsonPath("$.firstName").value("Grace Updated"));
        assertThat(candidateRows.count()).isEqualTo(2);

        mvc.perform(post("/api/v1/companies/profile").header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content(COMPANY_BODY))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/companies/profile").header("Authorization", "Bearer other-company")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Other Company"}
                                """))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/v1/companies/profile").header("Authorization", "Bearer other-company")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Updated Other Company"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/companies/profile/me").header("Authorization", "Bearer company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(COMPANY_USER.toString()))
                .andExpect(jsonPath("$.name").value("Acme"));
        mvc.perform(get("/api/v1/companies/profile/me").header("Authorization", "Bearer other-company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(OTHER_COMPANY_USER.toString()))
                .andExpect(jsonPath("$.name").value("Updated Other Company"));
        assertThat(companyRows.count()).isEqualTo(2);
    }

    @Test
    void internalLookupsReturnExactlyRequestedJsonWithCallerToken() throws Exception {
        mvc.perform(post("/api/v1/candidates/profile").header("Authorization", "Bearer candidate")
                        .contentType(MediaType.APPLICATION_JSON).content(CANDIDATE_BODY))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/companies/profile").header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content(COMPANY_BODY))
                .andExpect(status().isCreated());
        CandidateProfile candidate = candidates.getByUserId(CANDIDATE_USER);
        CompanyProfile company = companies.getByUserId(COMPANY_USER);

        mvc.perform(get("/internal/profiles/candidates/by-user/{userId}", CANDIDATE_USER)
                        .header("Authorization", "Bearer candidate"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":"%s","cvUrl":"https://example.org/cv.pdf"}
                        """.formatted(candidate.id()), JsonCompareMode.STRICT));
        String expectedCompany = """
                {"id":"%s","contactEmail":"jobs@acme.org"}
                """.formatted(company.id());
        mvc.perform(get("/internal/profiles/companies/by-user/{userId}", COMPANY_USER)
                        .header("Authorization", "Bearer company"))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedCompany, JsonCompareMode.STRICT));
        mvc.perform(get("/internal/profiles/companies/{id}", company.id())
                        .header("Authorization", "Bearer company"))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedCompany, JsonCompareMode.STRICT));
    }

    @Test
    void internalLookupsKeepNullableFieldsInJson() throws Exception {
        mvc.perform(post("/api/v1/candidates/profile").header("Authorization", "Bearer candidate")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"firstName":"Ada","lastName":"Lovelace"}
                                """))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/companies/profile").header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Acme\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/internal/profiles/candidates/by-user/{userId}", CANDIDATE_USER)
                        .header("Authorization", "Bearer candidate"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":"%s","cvUrl":null}
                        """.formatted(candidates.getByUserId(CANDIDATE_USER).id()), JsonCompareMode.STRICT));
        mvc.perform(get("/internal/profiles/companies/by-user/{userId}", COMPANY_USER)
                        .header("Authorization", "Bearer company"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":"%s","contactEmail":null}
                        """.formatted(companies.getByUserId(COMPANY_USER).id()), JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/internal/profiles/candidates/by-user/", "/internal/profiles/companies/by-user/",
            "/internal/profiles/companies/"
    })
    void missingInternalLookupReturnsNotFound(String path) throws Exception {
        mvc.perform(get(path + UUID.randomUUID()).header("Authorization", "Bearer candidate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:jolijob:error:not-found"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"candidates", "companies"})
    void requiresAuthenticationForAllExistingReadRoutes(String type) throws Exception {
        mvc.perform(get("/api/v1/" + type + "/profile/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/" + type + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void requiresAuthenticationForInternalProfileLookups() throws Exception {
        mvc.perform(get("/internal/profiles/candidates/by-user/{userId}", CANDIDATE_USER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restrictsCandidateRoutesToExistingRoles() throws Exception {
        mvc.perform(post("/api/v1/candidates/profile").header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content(CANDIDATE_BODY))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/candidates/profile").header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content(CANDIDATE_BODY))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization", "Bearer company"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/candidates/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer candidate"))
                .andExpect(status().isForbidden());
    }

    @Test
    void restrictsCompanyProfileRoutesToCompanyRole() throws Exception {
        mvc.perform(post("/api/v1/companies/profile").header("Authorization", "Bearer candidate")
                        .contentType(MediaType.APPLICATION_JSON).content(COMPANY_BODY))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/companies/profile").header("Authorization", "Bearer candidate")
                        .contentType(MediaType.APPLICATION_JSON).content(COMPANY_BODY))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/companies/profile/me").header("Authorization", "Bearer candidate"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validatesRequiredNames() throws Exception {
        mvc.perform(post("/api/v1/candidates/profile").header("Authorization", "Bearer candidate")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"firstName":" ","lastName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:jolijob:error:validation"));
        mvc.perform(post("/api/v1/companies/profile").header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(candidateRows.count()).isZero();
        assertThat(companyRows.count()).isZero();
    }

    @Test
    void duplicateProfilesReturnConflict() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/v1/candidates/profile").header("Authorization", "Bearer candidate")
                            .contentType(MediaType.APPLICATION_JSON).content(CANDIDATE_BODY))
                    .andExpect(attempt == 0 ? status().isCreated() : status().isConflict());
            mvc.perform(post("/api/v1/companies/profile").header("Authorization", "Bearer company")
                            .contentType(MediaType.APPLICATION_JSON).content(COMPANY_BODY))
                    .andExpect(attempt == 0 ? status().isCreated() : status().isConflict());
        }
        assertThat(candidateRows.count()).isEqualTo(1);
        assertThat(companyRows.count()).isEqualTo(1);
    }

    @Test
    void databaseRejectsDuplicateCandidateUserEvenWhenPrecheckIsBypassed() {
        CandidateDetails details = new CandidateDetails("Ada", "Lovelace", null, null, null,
                null, null, null, null, null, null, false, false);
        CandidateProfile original = candidates.create(CANDIDATE_USER, details);
        CandidateProfile duplicate = new CandidateProfile(UUID.randomUUID(), CANDIDATE_USER,
                details, Instant.now(), Instant.now());

        assertThatThrownBy(() -> candidateRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(candidateRepository.findByUserId(CANDIDATE_USER)).get()
                .extracting(CandidateProfile::id).isEqualTo(original.id());
        assertThat(candidateRows.count()).isEqualTo(1);
    }

    @Test
    void databaseRejectsDuplicateCompanyUserEvenWhenPrecheckIsBypassed() {
        CompanyDetails details = new CompanyDetails("Acme", null, null, null, null,
                null, null, null, null, null);
        CompanyProfile original = companies.create(COMPANY_USER, details);
        CompanyProfile duplicate = new CompanyProfile(UUID.randomUUID(), COMPANY_USER,
                details, Instant.now(), Instant.now());

        assertThatThrownBy(() -> companyRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(companyRepository.findByUserId(COMPANY_USER)).get()
                .extracting(CompanyProfile::id).isEqualTo(original.id());
        assertThat(companyRows.count()).isEqualTo(1);
    }

    @Test
    void missingOwnProfilesRetainConflictSemantics() throws Exception {
        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization", "Bearer candidate"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/candidates/profile").header("Authorization", "Bearer candidate")
                        .contentType(MediaType.APPLICATION_JSON).content(CANDIDATE_BODY))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/companies/profile/me").header("Authorization", "Bearer company"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/companies/profile").header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content(COMPANY_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void missingProfilesByIdReturnNotFound() throws Exception {
        mvc.perform(get("/api/v1/candidates/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer company"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/companies/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer candidate"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-sub", "malformed-sub"})
    void rejectsUnusableSubjectClaims(String token) throws Exception {
        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidTokensAndMalformedPathIds() throws Exception {
        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/profiles/companies/not-a-uuid").header("Authorization", "Bearer company"))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Tokens {
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> {
                if (!java.util.Set.of("candidate", "company", "other-candidate", "other-company",
                        "missing-sub", "malformed-sub").contains(token)) {
                    throw new BadJwtException("Invalid test token");
                }
                Jwt.Builder builder = Jwt.withTokenValue(token).header("alg", "RS256")
                        .issuer("http://localhost:8081/realms/jolijob")
                        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                        .claim("email", "user@example.org").claim("email_verified", true)
                        .claim("aud", java.util.List.of("jolijob-api"))
                        .claim("realm_access", java.util.Map.of("roles",
                                java.util.List.of(token.equals("company") || token.equals("other-company")
                                        ? "COMPANY" : "CANDIDATE")));
                if (!token.equals("missing-sub")) {
                    builder.subject(switch (token) {
                        case "company" -> COMPANY_USER.toString();
                        case "other-candidate" -> OTHER_CANDIDATE_USER.toString();
                        case "other-company" -> OTHER_COMPANY_USER.toString();
                        case "malformed-sub" -> "not-a-uuid";
                        default -> CANDIDATE_USER.toString();
                    });
                }
                return builder.build();
            };
        }
    }
}
