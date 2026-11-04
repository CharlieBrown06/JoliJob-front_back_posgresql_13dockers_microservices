package com.example.jolijob.joboffer.adapter.config;

import com.example.jolijob.joboffer.application.port.in.JobOfferUseCases;
import com.example.jolijob.joboffer.application.port.out.CompanyPort;
import com.example.jolijob.joboffer.application.port.out.JobOfferEventPort;
import com.example.jolijob.joboffer.application.port.out.JobOfferRepositoryPort;
import com.example.jolijob.joboffer.application.service.JobOfferService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class JobOfferConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    JobOfferUseCases jobOfferUseCases(JobOfferRepositoryPort repository, CompanyPort companies,
                                      JobOfferEventPort events, Clock clock) {
        return new TransactionalJobOfferUseCases(
                new JobOfferService(repository, companies, events, clock), repository);
    }
}
