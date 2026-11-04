package com.example.jolijob.search;

import com.example.jolijob.search.application.SearchService;
import com.example.jolijob.search.application.port.JobOfferSearchPort;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }

    @Bean
    SearchService searchService(JobOfferSearchPort searchPort) {
        return new SearchService(searchPort);
    }
}
