package com.example.jolijob.profile.adapter.in.rest;

import com.example.jolijob.profile.config.SecurityConfiguration;
import com.example.jolijob.profile.domain.CandidateDetails;
import com.example.jolijob.profile.domain.CandidateProfile;
import com.example.jolijob.profile.application.port.in.CandidateProfileUseCase;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CandidateController.class)
@Import(SecurityConfiguration.class)
class ProfileJwtValidationTest {
    private static final String ISSUER = "http://localhost:8081/realms/jolijob";
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000123");
    private static HttpServer jwks;
    private static RSAKey key;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CandidateProfileUseCase profiles;

    @BeforeAll
    static void startJwks() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("profile-test-key").generate();
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
    void acceptsKeycloakRealmRoleAndUsesUuidSubjectAsProfileOwner() throws Exception {
        CandidateProfile profile = new CandidateProfile(UUID.randomUUID(), USER_ID,
                new CandidateDetails("Ada", "Lovelace", null, null, null, null, null,
                        null, null, null, null, false, false), Instant.now(), Instant.now());
        when(profiles.getByUserId(USER_ID)).thenReturn(profile);

        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization",
                        "Bearer " + token(ISSUER, "jolijob-api", Instant.now().plusSeconds(300),
                                JWSAlgorithm.RS256, key, "CANDIDATE", USER_ID.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));

        verify(profiles).getByUserId(USER_ID);
    }

    @Test
    void rejectsWrongIssuerAndAudience() throws Exception {
        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization",
                        "Bearer " + token("http://untrusted-issuer", "jolijob-api",
                                Instant.now().plusSeconds(300), JWSAlgorithm.RS256, key,
                                "CANDIDATE", USER_ID.toString())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization",
                        "Bearer " + token(ISSUER, "another-api", Instant.now().plusSeconds(300),
                                JWSAlgorithm.RS256, key, "CANDIDATE", USER_ID.toString())))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(profiles);
    }

    @Test
    void rejectsExpiredAndNonRs256Tokens() throws Exception {
        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization",
                        "Bearer " + token(ISSUER, "jolijob-api", Instant.now().minusSeconds(300),
                                JWSAlgorithm.RS256, key, "CANDIDATE", USER_ID.toString())))
                .andExpect(status().isUnauthorized());

        var claims = claims(ISSUER, "jolijob-api", Instant.now().plusSeconds(300),
                "CANDIDATE", USER_ID.toString());
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new MACSigner(new byte[32]));
        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization", "Bearer " + jwt.serialize()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(profiles);
    }

    @Test
    void rejectsTokenWithoutExpiration() throws Exception {
        mvc.perform(get("/api/v1/candidates/profile/me").header("Authorization",
                        "Bearer " + token(ISSUER, "jolijob-api", null,
                                JWSAlgorithm.RS256, key, "CANDIDATE", USER_ID.toString())))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(profiles);
    }

    private static String token(String issuer, String audience, Instant expiresAt, JWSAlgorithm algorithm,
                                RSAKey signingKey, String role, String subject) throws Exception {
        var jwt = new SignedJWT(new JWSHeader.Builder(algorithm)
                .type(JOSEObjectType.JWT).keyID(signingKey.getKeyID()).build(),
                claims(issuer, audience, expiresAt, role, subject));
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    private static JWTClaimsSet claims(String issuer, String audience, Instant expiresAt,
                                       String role, String subject) {
        var builder = new JWTClaimsSet.Builder().issuer(issuer).subject(subject).audience(audience)
                .claim("email", "candidate@example.com").claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .issueTime(Date.from(Instant.now().minusSeconds(60)));
        if (expiresAt != null) {
            builder.expirationTime(Date.from(expiresAt));
        }
        return builder.build();
    }
}
