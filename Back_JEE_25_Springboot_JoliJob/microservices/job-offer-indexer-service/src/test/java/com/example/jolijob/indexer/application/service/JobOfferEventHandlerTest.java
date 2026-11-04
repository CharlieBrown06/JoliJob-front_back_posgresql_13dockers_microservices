package com.example.jolijob.indexer.application.service;

import com.example.jolijob.indexer.application.domain.JobOfferEvent;
import com.example.jolijob.indexer.application.domain.JobOfferEventType;
import com.example.jolijob.indexer.application.port.out.JobOfferIndexPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JobOfferEventHandlerTest {

    private final JobOfferIndexPort indexPort = mock(JobOfferIndexPort.class);
    private final JobOfferEventHandler handler = new JobOfferEventHandler(indexPort);

    @Test
    void createdOfferIsSavedAsActive() {
        handler.process(event("CREATED"));

        verify(indexPort).index(org.mockito.ArgumentMatchers.argThat(offer ->
                offer.id().equals("offer-1") && offer.active()));
    }

    @Test
    void updatedOfferUsesThePartialUpdatePort() {
        handler.process(event("UPDATED"));

        verify(indexPort).update(org.mockito.ArgumentMatchers.argThat(offer ->
                offer.id().equals("offer-1") && offer.title().equals("Développeur")));
    }

    @Test
    void deletedOfferIsDeactivated() {
        handler.process(event("DELETED"));

        verify(indexPort).deactivate("offer-1");
    }

    private JobOfferEvent event(String type) {
        return new JobOfferEvent(
                JobOfferEventType.valueOf(type), "offer-1", "company-1", "JoliJob", "Développeur",
                "Description", "IT", "Java", "CDI", true, "France",
                "Île-de-France", "Paris", "Paris", BigDecimal.valueOf(40000),
                BigDecimal.valueOf(50000), Instant.parse("2026-10-20T00:00:00Z"));
    }
}
