package com.example.jolijob.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayRoutingTest {
    private static final HttpServer PROFILE = backend("profile");
    private static final HttpServer JOB_OFFERS = backend("job-offers");
    private static final HttpServer APPLICATIONS = backend("applications");
    private static final HttpServer SEARCH = backend("search");

    @LocalServerPort
    private int port;

    private WebTestClient client;

    @DynamicPropertySource
    static void backends(DynamicPropertyRegistry registry) {
        registry.add("PROFILE_SERVICE_URL", () -> url(PROFILE));
        registry.add("JOB_OFFER_SERVICE_URL", () -> url(JOB_OFFERS));
        registry.add("APPLICATION_SERVICE_URL", () -> url(APPLICATIONS));
        registry.add("SEARCH_SERVICE_URL", () -> url(SEARCH));
    }

    @BeforeEach
    void createClient() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
    }

    @AfterAll
    static void stopBackends() {
        List.of(PROFILE, JOB_OFFERS, APPLICATIONS, SEARCH)
                .forEach(server -> server.stop(0));
    }

    @ParameterizedTest
    @CsvSource({
            "candidates, profile",
            "companies, profile",
            "job-offers, job-offers",
            "applications, applications",
            "search, search"
    })
    void routesBaseTrailingSlashAndNestedPathsWithoutStrippingPrefix(String resource, String backend) {
        for (String suffix : List.of("", "/", "/nested/42")) {
            String path = "/api/v1/" + resource + suffix + "?page=2&sort=recent";
            client.get().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer sample-token")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .isEqualTo(backend + "|GET|" + path + "|Bearer sample-token|");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void preservesMethodBodyAndAuthorization(String method) {
        String body = "{\"name\":\"example\"}";
        client.method(org.springframework.http.HttpMethod.valueOf(method))
                .uri("/api/v1/candidates/42")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer sample-token")
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("profile|" + method
                        + "|/api/v1/candidates/42|Bearer sample-token|" + body);
    }

    @Test
    void preservesEncodedPathAndQuery() {
        String path = "/api/v1/search/hello%20world?q=java%2Bspring&tag=a&tag=b";
        client.get().uri(java.net.URI.create("http://127.0.0.1:" + port + path)).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("search|GET|" + path + "|null|");
    }

    @Test
    void propagatesUpstreamErrorStatusHeadersAndBody() {
        client.get().uri("/api/v1/job-offers/unavailable").exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "10")
                .expectBody(String.class)
                .isEqualTo("job-offers|GET|/api/v1/job-offers/unavailable|null|");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 500})
    void preservesBusinessAndAuthenticationErrorsWithoutRedirecting(int status) {
        client.get().uri("/api/v1/applications/status/" + status).exchange()
                .expectStatus().isEqualTo(status)
                .expectHeader().doesNotExist(HttpHeaders.LOCATION)
                .expectBody(String.class)
                .isEqualTo("applications|GET|/api/v1/applications/status/" + status + "|null|");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/internal/profiles/candidates/42", "/internal/job-offers/42"})
    void doesNotExposeInternalServiceEndpoints(String path) {
        client.get().uri(path).exchange().expectStatus().isNotFound();
    }

    @Test
    void forwardsTheResourceServerBearerChallenge() {
        client.get().uri("/api/v1/applications/status/401").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer error=\"invalid_token\"")
                .expectHeader().doesNotExist(HttpHeaders.LOCATION);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/", "/api/v1/unknown", "/api/v1/authentication/login",
            "/api/v1/auth/login", "/api/v1/auth/register/candidate", "/api/v1/auth/register/company",
            "/realms/jolijob/protocol/openid-connect/token",
            "/api/v1/candidates-other/42", "/api/v2/search",
            "/oauth2/authorize", "/oauth2/token", "/oauth2/jwks",
            "/.well-known/openid-configuration", "/login", "/logout"
    })
    void rejectsUnmappedPathsWithoutOAuthRedirects(String path) {
        client.get().uri(path).exchange()
                .expectStatus().isNotFound()
                .expectHeader().doesNotExist(HttpHeaders.LOCATION);
    }

    private static String url(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static HttpServer backend(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                try (exchange) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    byte[] response = (name + "|" + exchange.getRequestMethod() + "|"
                            + exchange.getRequestURI() + "|"
                            + exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION)
                            + "|" + body).getBytes(StandardCharsets.UTF_8);
                    boolean unavailable = exchange.getRequestURI().getPath().endsWith("/unavailable");
                    exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
                    if (unavailable) {
                        exchange.getResponseHeaders().set(HttpHeaders.RETRY_AFTER, "10");
                    }
                    String path = exchange.getRequestURI().getPath();
                    int status = path.contains("/status/")
                            ? Integer.parseInt(path.substring(path.lastIndexOf('/') + 1))
                            : unavailable ? 503 : 200;
                    if (status == 401) {
                        exchange.getResponseHeaders().set(HttpHeaders.WWW_AUTHENTICATE,
                                "Bearer error=\"invalid_token\"");
                    }
                    exchange.sendResponseHeaders(status, response.length);
                    exchange.getResponseBody().write(response);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot start test backend " + name, exception);
        }
    }
}
