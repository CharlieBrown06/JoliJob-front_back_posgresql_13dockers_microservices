package com.example.jolijob.indexer.adapter.config;

import com.example.jolijob.indexer.application.port.in.ProcessJobOfferEvent;
import com.example.jolijob.indexer.application.port.out.JobOfferIndexPort;
import com.example.jolijob.indexer.application.service.JobOfferEventHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobOfferIndexerConfiguration {

    @Bean
    ProcessJobOfferEvent processJobOfferEvent(JobOfferIndexPort jobOfferIndexPort) {
        return new JobOfferEventHandler(jobOfferIndexPort);
    }
}
