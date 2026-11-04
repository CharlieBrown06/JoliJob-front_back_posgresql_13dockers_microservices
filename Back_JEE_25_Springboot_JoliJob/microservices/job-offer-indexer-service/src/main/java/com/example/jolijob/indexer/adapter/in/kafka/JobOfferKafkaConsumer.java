package com.example.jolijob.indexer.adapter.in.kafka;

import com.example.jolijob.indexer.application.domain.JobOfferEvent;
import com.example.jolijob.indexer.application.domain.JobOfferEventType;
import com.example.jolijob.indexer.application.port.in.ProcessJobOfferEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobOfferKafkaConsumer {

    private final ProcessJobOfferEvent processJobOfferEvent;

    public JobOfferKafkaConsumer(ProcessJobOfferEvent processJobOfferEvent) {
        this.processJobOfferEvent = processJobOfferEvent;
    }

    @KafkaListener(topics = "job-offer.events", groupId = "jolijob-indexer")
    public void consume(JobOfferEventMessage message) {
        processJobOfferEvent.process(new JobOfferEvent(
                JobOfferEventType.valueOf(message.type()),
                message.jobOfferId(),
                message.companyId(),
                message.companyName(),
                message.title(),
                message.description(),
                message.jobCategory(),
                message.specialty(),
                message.contractType(),
                message.remote(),
                message.country(),
                message.region(),
                message.department(),
                message.city(),
                message.salaryMin(),
                message.salaryMax(),
                message.occurredAt()));
    }
}
