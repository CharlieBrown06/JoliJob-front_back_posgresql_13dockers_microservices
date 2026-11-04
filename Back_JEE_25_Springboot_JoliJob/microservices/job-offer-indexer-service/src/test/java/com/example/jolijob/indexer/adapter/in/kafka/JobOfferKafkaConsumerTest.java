package com.example.jolijob.indexer.adapter.in.kafka;

import com.example.jolijob.indexer.application.domain.JobOfferEvent;
import com.example.jolijob.indexer.application.domain.JobOfferEventType;
import com.example.jolijob.indexer.application.port.in.ProcessJobOfferEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class JobOfferKafkaConsumerTest {

    private final ProcessJobOfferEvent useCase = mock(ProcessJobOfferEvent.class);
    private final JobOfferKafkaConsumer consumer = new JobOfferKafkaConsumer(useCase);
    private final Instant occurredAt = Instant.parse("2026-10-20T12:34:56.123Z");

    @ParameterizedTest
    @EnumSource(JobOfferEventType.class)
    void mapsAllFieldsAndEverySupportedType(JobOfferEventType type) {
        consumer.consume(message(type.name()));

        verify(useCase).process(new JobOfferEvent(type, "offer-42", "company-42", "JoliJob",
                "Développeur", "Description", "IT", "Java", "CDI", true, "France",
                "Île-de-France", "75", "Paris", new BigDecimal("40000.50"),
                new BigDecimal("60000.75"), occurredAt));
    }

    @Test
    void rejectsUnknownTypesBeforeCallingUseCase() {
        assertThatThrownBy(() -> consumer.consume(message("UNKNOWN")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(useCase);
    }

    private JobOfferEventMessage message(String type) {
        return new JobOfferEventMessage("event-42", type, "offer-42", "company-42", "JoliJob",
                "Développeur", "Description", "IT", "Java", "CDI", true, "France",
                "Île-de-France", "75", "Paris", new BigDecimal("40000.50"),
                new BigDecimal("60000.75"), occurredAt);
    }
}
