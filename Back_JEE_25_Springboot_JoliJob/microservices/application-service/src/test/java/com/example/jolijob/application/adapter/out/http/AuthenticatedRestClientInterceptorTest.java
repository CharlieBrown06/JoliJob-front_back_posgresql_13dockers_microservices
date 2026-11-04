package com.example.jolijob.application.adapter.out.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthenticatedRestClientInterceptorTest {
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void forwardsAuthenticatedUsersBearerToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://service")
                .requestInterceptor(new AuthenticatedRestClientInterceptor());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("caller-token").header("alg", "RS256")
                        .subject("00000000-0000-0000-0000-000000000001").build(),
                java.util.List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))));
        server.expect(requestTo("http://service/internal/lookup"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer caller-token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        builder.build().get().uri("/internal/lookup").retrieve().toBodilessEntity();

        server.verify();
    }

    @Test
    void refusesInternalRequestsWithoutAuthenticatedJwt() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://service")
                .requestInterceptor(new AuthenticatedRestClientInterceptor());
        RestClient client = builder.build();

        assertThatThrownBy(() -> client.get().uri("/internal/lookup").retrieve().toBodilessEntity())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Internal service lookup requires the authenticated caller's token");
    }
}
