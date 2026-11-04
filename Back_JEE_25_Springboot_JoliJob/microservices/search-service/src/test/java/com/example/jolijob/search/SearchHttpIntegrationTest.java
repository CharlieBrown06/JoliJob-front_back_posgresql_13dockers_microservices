package com.example.jolijob.search;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchHttpIntegrationTest {
    private static final String EMPTY_RESULT = """
            {"took":1,"timed_out":false,"_shards":{"total":1,"successful":1,"skipped":0,"failed":0},
             "hits":{"total":{"value":0,"relation":"eq"},"max_score":null,"hits":[]}}
            """;
    private static final AtomicReference<String> RESPONSE = new AtomicReference<>(EMPTY_RESULT);
    private static final AtomicInteger STATUS = new AtomicInteger(200);
    private static final AtomicReference<String> SEARCH_BODY = new AtomicReference<>();
    private static final AtomicReference<String> SEARCH_PATH = new AtomicReference<>();
    private static final HttpServer ELASTICSEARCH = startBackend();
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void backend(DynamicPropertyRegistry properties) {
        properties.add("spring.elasticsearch.uris",
                () -> "http://127.0.0.1:" + ELASTICSEARCH.getAddress().getPort());
    }

    @BeforeEach
    void resetBackend() {
        RESPONSE.set(EMPTY_RESULT);
        STATUS.set(200);
        SEARCH_BODY.set(null);
        SEARCH_PATH.set(null);
    }

    @AfterAll
    static void stopBackend() {
        ELASTICSEARCH.stop(0);
    }

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void searchesThroughTheRealHttpAndElasticsearchAdapters() throws Exception {
        RESPONSE.set("""
                {"took":1,"timed_out":false,
                 "_shards":{"total":1,"successful":1,"skipped":0,"failed":0},
                 "hits":{"total":{"value":12,"relation":"eq"},"max_score":1.0,
                 "hits":[{"_index":"job_offers","_id":"offer-1","_score":1.0,"_source":{
                   "id":"offer-1","companyId":"company-1","companyName":"Acme",
                   "title":"Java developer","description":"Build APIs","jobCategory":"IT",
                   "specialty":"Java","contractType":"CDI","remote":false,"country":"France",
                   "region":"IDF","department":"75","city":"Paris","salaryMin":45000.50,
                   "salaryMax":65000.75,"active":true,"createdAt":"2026-10-20T12:00:00Z"}}]}}
                """);

        var response = get("?keyword=Java&remote=false&page=1&size=5");

        assertThat(response.statusCode()).isEqualTo(200);
        var body = json.readTree(response.body());
        assertThat(body.path("totalHits").asLong()).isEqualTo(12);
        assertThat(body.path("page").asInt()).isEqualTo(1);
        assertThat(body.path("size").asInt()).isEqualTo(5);
        assertThat(body.at("/results/0/id").asText()).isEqualTo("offer-1");
        assertThat(body.at("/results/0/companyName").asText()).isEqualTo("Acme");
        assertThat(body.at("/results/0/salaryMin").decimalValue()).isEqualByComparingTo("45000.50");
        assertThat(body.at("/results/0/remote").asBoolean()).isFalse();
        assertThat(SEARCH_PATH.get()).isEqualTo("/job_offers/_search");
        var query = json.readTree(SEARCH_BODY.get());
        assertThat(query.path("from").asInt()).isEqualTo(5);
        assertThat(query.path("size").asInt()).isEqualTo(5);
        assertThat(query.at("/query/bool/must").toString())
                .contains("\"active\"", "\"value\":true", "\"remote\"", "\"value\":false", "\"Java\"");
    }

    @Test
    void emptyBackendResultRemainsAnEmptySuccessfulPage() throws Exception {
        var response = get("");

        assertThat(response.statusCode()).isEqualTo(200);
        var body = json.readTree(response.body());
        assertThat(body.path("results").isArray()).isTrue();
        assertThat(body.path("results").size()).isZero();
        assertThat(body.path("totalHits").asLong()).isZero();
        assertThat(SEARCH_BODY.get()).isNotNull();
    }

    @Test
    void invalidQueryDoesNotReachElasticsearch() throws Exception {
        assertThat(get("?salaryMin=not-a-number").statusCode()).isEqualTo(400);
        assertThat(SEARCH_BODY.get()).isNull();
    }

    @Test
    void backendFailureDoesNotMasqueradeAsAnEmptySuccessfulSearch() throws Exception {
        STATUS.set(503);
        RESPONSE.set("""
                {"error":{"type":"unavailable_shards_exception","reason":"No available shards"},"status":503}
                """);

        assertThat(get("").statusCode()).isEqualTo(500);
        assertThat(SEARCH_BODY.get()).isNotNull();
    }

    private HttpResponse<String> get(String query) throws Exception {
        return client.send(HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/v1/search/job-offers" + query))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer startBackend() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                try (exchange) {
                    SEARCH_PATH.set(exchange.getRequestURI().getPath());
                    SEARCH_BODY.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    byte[] bytes = RESPONSE.get().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("X-Elastic-Product", "Elasticsearch");
                    exchange.sendResponseHeaders(STATUS.get(), bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot start Elasticsearch HTTP fixture", exception);
        }
    }
}
