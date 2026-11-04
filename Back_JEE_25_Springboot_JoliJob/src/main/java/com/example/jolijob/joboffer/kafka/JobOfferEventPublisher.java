package com.example.jolijob.joboffer.kafka;

import com.example.jolijob.joboffer.domain.JobOffer;
import com.example.jolijob.shared.event.JobOfferEventType;
import com.example.jolijob.shared.event.JobOfferKafkaEvent;
import com.example.jolijob.shared.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobOfferEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(JobOffer offer, JobOfferEventType type) {
        JobOfferKafkaEvent event = new JobOfferKafkaEvent(
                UUID.randomUUID().toString(),
                type,
                offer.getId().toString(),
                offer.getCompanyId().toString(),
                offer.getCompanyName(),
                offer.getTitle(),
                offer.getDescription(),
                offer.getJobCategory(),
                offer.getSpecialty(),
                offer.getContractType().name(),
                offer.isRemote(),
                offer.getCountry(),
                offer.getRegion(),
                offer.getDepartment(),
                offer.getCity(),
                offer.getSalaryMin(),
                offer.getSalaryMax(),
                Instant.now()
        );
        kafkaTemplate.send(KafkaTopics.JOB_OFFER_EVENTS, offer.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Erreur publication Kafka job-offer event {}", offer.getId(), ex);
                    } else {
                        log.debug("Job-offer event publié : {} type={}", offer.getId(), type);
                    }
                });
    }
}
