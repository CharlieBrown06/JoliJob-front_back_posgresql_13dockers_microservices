package com.example.jolijob.application.adapter.out.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ProfileRestClientConfiguration {
    @Bean("profileServiceRestClient")
    RestClient profileServiceRestClient(@Value("${services.profiles.base-url}") String baseUrl,
                                        AuthenticatedRestClientInterceptor interceptor) {
        return RestClient.builder().baseUrl(baseUrl).requestInterceptor(interceptor).build();
    }

    @Bean("jobOfferServiceRestClient")
    RestClient jobOfferServiceRestClient(@Value("${services.job-offers.base-url}") String baseUrl,
                                         AuthenticatedRestClientInterceptor interceptor) {
        return RestClient.builder().baseUrl(baseUrl).requestInterceptor(interceptor).build();
    }
}
