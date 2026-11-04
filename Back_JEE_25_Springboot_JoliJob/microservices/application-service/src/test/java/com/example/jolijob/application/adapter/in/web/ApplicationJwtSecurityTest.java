package com.example.jolijob.application.adapter.in.web;

import com.example.jolijob.application.adapter.config.SecurityConfiguration;
import com.example.jolijob.application.domain.Actor;
import com.example.jolijob.application.port.in.ApplicationUseCase;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfiguration.class)
class ApplicationJwtSecurityTest {
    private static final String ISSUER = "http://localhost:8081/realms/jolijob";
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000123");
    private static final UUID OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000456");
    private static HttpServer jwks;
    private static RSAKey key;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ApplicationUseCase applications;

    @BeforeAll
    static void startJwks() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("application-test-key").generate();
        byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        jwks = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwks.createContext("/certs", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var response = exchange.getResponseBody()) {
                response.write(body);
            }
        });
        jwks.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + jwks.getAddress().getPort() + "/certs");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
    }

    @AfterAll
    static void stopJwks() {
        if (jwks != null) {
            jwks.stop(0);
        }
    }

    @Test
    void extractsStableSubjectAndEmailAndSelectsCompanyRoleFromMultipleRealmRoles() throws Exception {
        when(applications.getMyCandidateApplications(new Actor(USER_ID, "candidate@example.com", "CANDIDATE")))
                .thenReturn(List.of());
        mvc.perform(get("/api/v1/applications/mine").header("Authorization",
                        "Bearer " + token(List.of("CANDIDATE"), USER_ID.toString(),
                                "candidate@example.com", true, "jolijob-api", Instant.now().plusSeconds(300))))
                .andExpect(status().isOk());
        verify(applications).getMyCandidateApplications(
                new Actor(USER_ID, "candidate@example.com", "CANDIDATE"));

        when(applications.getOfferApplications(new Actor(USER_ID, "candidate@example.com", "COMPANY"), OFFER_ID))
                .thenReturn(List.of());
        mvc.perform(get("/api/v1/applications/offer/" + OFFER_ID).header("Authorization",
                        "Bearer " + token(List.of("CANDIDATE", "COMPANY"), USER_ID.toString(),
                                "candidate@example.com", true, "jolijob-api", Instant.now().plusSeconds(300))))
                .andExpect(status().isOk());
        verify(applications).getOfferApplications(
                new Actor(USER_ID, "candidate@example.com", "COMPANY"), OFFER_ID);
    }

    @Test
    void rejectsUnverifiedCandidateEmailDuringAuthentication() throws Exception {
        mvc.perform(get("/api/v1/applications/mine").header("Authorization",
                        "Bearer " + token(List.of("CANDIDATE"), USER_ID.toString(),
                                "candidate@example.com", false, "jolijob-api", Instant.now().plusSeconds(300))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsNonUuidSubjectDuringAuthentication() throws Exception {
        mvc.perform(get("/api/v1/applications/mine").header("Authorization",
                        "Bearer " + token(List.of("CANDIDATE"), "not-a-uuid",
                                "candidate@example.com", true, "jolijob-api", Instant.now().plusSeconds(300))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithoutExpiration() throws Exception {
        mvc.perform(get("/api/v1/applications/mine").header("Authorization",
                        "Bearer " + token(List.of("CANDIDATE"), USER_ID.toString(),
                                "candidate@example.com", true, "jolijob-api", null)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(applications);
    }

    @Test
    void rejectsMalformedEmailClaimAsInvalidToken() throws Exception {
        mvc.perform(get("/api/v1/applications/mine").header("Authorization",
                        "Bearer " + token(List.of("CANDIDATE"), USER_ID.toString(),
                                123, true, "jolijob-api", Instant.now().plusSeconds(300))))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(applications);
    }

    @Test
    void rejectsIdTokenAudienceEvenWhenOtherwiseValid() throws Exception {
        mvc.perform(get("/api/v1/applications/mine").header("Authorization",
                        "Bearer " + token(List.of("CANDIDATE"), USER_ID.toString(),
                                "candidate@example.com", true, "jolijob-web",
                                Instant.now().plusSeconds(300))))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(applications);
    }

    @Test
    void rejectsUnrecognizedRealmRole() throws Exception {
        mvc.perform(get("/api/v1/applications/mine").header("Authorization",
                        "Bearer " + token(List.of("default-roles-jolijob"), USER_ID.toString(),
                                "candidate@example.com", true, "jolijob-api",
                                Instant.now().plusSeconds(300))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(applications);
    }

    private static String token(List<String> roles, String subject, Object email, boolean emailVerified,
                                String audience, Instant expiresAt) throws Exception {
        var claimsBuilder = new JWTClaimsSet.Builder().issuer(ISSUER).subject(subject).audience(audience)
                .claim("email", email).claim("email_verified", emailVerified)
                .claim("realm_access", java.util.Map.of("roles", roles))
                .issueTime(Date.from(Instant.now().minusSeconds(60)));
        if (expiresAt != null) {
            claimsBuilder.expirationTime(Date.from(expiresAt));
        }
        var claims = claimsBuilder.build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
