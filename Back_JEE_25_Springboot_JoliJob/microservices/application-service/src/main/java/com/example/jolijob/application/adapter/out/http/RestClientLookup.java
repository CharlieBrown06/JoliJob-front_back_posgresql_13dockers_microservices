package com.example.jolijob.application.adapter.out.http;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

final class RestClientLookup {
    private RestClientLookup() {
    }

    static <T> Optional<T> get(RestClient client, String uri, Class<T> responseType, Object... uriVariables) {
        try {
            return Optional.ofNullable(client.get().uri(uri, uriVariables).retrieve().body(responseType));
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        }
    }
}
