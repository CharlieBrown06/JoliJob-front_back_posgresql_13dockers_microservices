package com.example.jolijob.joboffer.adapter.in.rest;

import com.example.jolijob.joboffer.adapter.config.SecurityConfiguration;
import com.example.jolijob.joboffer.application.port.in.JobOfferUseCases;
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
import org.junit.jupiter.api.BeforeEach;
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

import static com.example.jolijob.joboffer.OfferFixture.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobOfferController.class)
@Import(SecurityConfiguration.class)
class JwtValidationTest {
    private static HttpServer jwks;
    private static RSAKey key;
    @Autowired MockMvc mvc;
    @MockitoBean JobOfferUseCases offers;

    @BeforeAll
    static void startJwks() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        jwks = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwks.createContext("/jwks", exchange -> {
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
                () -> "http://127.0.0.1:" + jwks.getAddress().getPort() + "/jwks");
    }

    @AfterAll
    static void stopJwks() {
        if (jwks != null) {
            jwks.stop(0);
        }
    }

    @BeforeEach
    void setup() {
        when(offers.getById(OFFER_ID)).thenReturn(offer());
        when(offers.getMyOffers(USER_ID)).thenReturn(List.of());
    }

    @Test
    void acceptsValidSignatureAndConfiguredIssuer() throws Exception {
        mvc.perform(get("/api/v1/job-offers/" + OFFER_ID).header("Authorization",
                        "Bearer " + token("http://localhost:8081/realms/jolijob",
                                Instant.now().plusSeconds(300), key)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsWrongIssuerEvenWithValidSignature() throws Exception {
        mvc.perform(get("/api/v1/job-offers/" + OFFER_ID).header("Authorization",
                        "Bearer " + token("http://untrusted-issuer", Instant.now().plusSeconds(300), key)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(offers);
    }

    @Test
    void rejectsWrongAudienceEvenWithValidSignature() throws Exception {
        mvc.perform(get("/api/v1/job-offers/" + OFFER_ID).header("Authorization",
                        "Bearer " + token("http://localhost:8081/realms/jolijob",
                                Instant.now().plusSeconds(300), key, List.of("COMPANY"), USER_ID.toString(),
                                "another-api")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(offers);
    }

    @Test
    void rejectsExpiredTokens() throws Exception {
        mvc.perform(get("/api/v1/job-offers/" + OFFER_ID).header("Authorization",
                        "Bearer " + token("http://localhost:8081/realms/jolijob",
                                Instant.now().minusSeconds(300), key)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(offers);
    }

    @Test
    void rejectsTokensWithoutExpiration() throws Exception {
        mvc.perform(get("/api/v1/job-offers/" + OFFER_ID).header("Authorization",
                        "Bearer " + token("http://localhost:8081/realms/jolijob",
                                null, key)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(offers);
    }

    @Test
    void rejectsUntrustedSignature() throws Exception {
        var otherKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        mvc.perform(get("/api/v1/job-offers/" + OFFER_ID).header("Authorization",
                        "Bearer " + token("http://localhost:8081/realms/jolijob",
                                Instant.now().plusSeconds(300), otherKey)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(offers);
    }

    @Test
    void rejectsUnsupportedSigningAlgorithm() throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer("http://localhost:8081/realms/jolijob")
                .subject(USER_ID.toString()).audience("jolijob-api")
                .issueTime(Date.from(Instant.now().minusSeconds(600)))
                .expirationTime(Date.from(Instant.now().plusSeconds(300))).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT).keyID("test-key").build(), claims);
        jwt.sign(new MACSigner(new byte[32]));
        mvc.perform(get("/api/v1/job-offers/" + OFFER_ID).header("Authorization", "Bearer " + jwt.serialize()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(offers);
    }

    @Test
    void mapsKeycloakRealmRolesAndUsesUuidSubjectAsActorId() throws Exception {
        mvc.perform(get("/api/v1/job-offers/mine").header("Authorization",
                        "Bearer " + token("http://localhost:8081/realms/jolijob",
                                Instant.now().plusSeconds(300), key, List.of("COMPANY"), USER_ID.toString(),
                                "jolijob-api")))
                .andExpect(status().isOk());
        verify(offers).getMyOffers(USER_ID);
    }

    private static String token(String issuer, Instant expiresAt, RSAKey signingKey) throws Exception {
        return token(issuer, expiresAt, signingKey, List.of("CANDIDATE"), USER_ID.toString(), "jolijob-api");
    }

    private static String token(String issuer, Instant expiresAt, RSAKey signingKey,
                                List<String> roles, String subject, String audience) throws Exception {
        var claimsBuilder = new JWTClaimsSet.Builder().issuer(issuer).subject(subject).audience(audience)
                .claim("email", "candidate@example.com").claim("email_verified", true)
                .claim("realm_access", java.util.Map.of("roles", roles))
                .issueTime(Date.from(Instant.now().minusSeconds(600)));
        if (expiresAt != null) {
            claimsBuilder.expirationTime(Date.from(expiresAt));
        }
        var claims = claimsBuilder.build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT).keyID(signingKey.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }
}
