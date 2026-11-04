package com.example.jolijob.application.adapter.out.kafka;

import com.example.jolijob.application.adapter.out.persistence.ApplicationOutboxEntity;
import com.example.jolijob.application.adapter.out.persistence.SpringDataApplicationOutboxRepository;
import com.example.jolijob.application.domain.ApplicationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class ApplicationOutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(ApplicationOutboxRelay.class);
    private static final String TOPIC = "application.events";
    private static final int MAX_ERROR_LENGTH = 2000;
    private static final long MAX_RETRY_DELAY_SECONDS = 300;

    private final SpringDataApplicationOutboxRepository outbox;
    private final KafkaTemplate<String, ApplicationEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final long acknowledgementTimeoutSeconds;

    public ApplicationOutboxRelay(
            SpringDataApplicationOutboxRepository outbox,
            KafkaTemplate<String, ApplicationEvent> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${application.outbox.batch-size:100}") int batchSize,
            @Value("${application.outbox.acknowledgement-timeout-seconds:10}") long acknowledgementTimeoutSeconds) {
        if (batchSize < 1 || acknowledgementTimeoutSeconds < 1) {
            throw new IllegalArgumentException("Outbox batch size and acknowledgement timeout must be positive");
        }
        this.outbox = outbox;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.acknowledgementTimeoutSeconds = acknowledgementTimeoutSeconds;
    }

    @Scheduled(fixedDelayString = "${application.outbox.relay-delay-ms:1000}")
    @Transactional
    public void relay() {
        for (ApplicationOutboxEntity entry : outbox.claimNextBatch(Instant.now(), batchSize)) {
            try {
                ApplicationEvent event = objectMapper.readValue(entry.getPayload(), ApplicationEvent.class);
                kafkaTemplate.send(TOPIC, event.applicationId(), event)
                        .get(acknowledgementTimeoutSeconds, TimeUnit.SECONDS);
                entry.markPublished(Instant.now());
                log.info("Published application event {}", event.eventId());
            } catch (ExecutionException | TimeoutException | RuntimeException exception) {
                persistRetry(entry, exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                persistRetry(entry, exception);
                return;
            }
        }
    }

    private void persistRetry(ApplicationOutboxEntity entry, Exception failure) {
        Instant retryAt = nextAttempt(entry);
        int attempts = entry.recordFailure(retryAt, errorMessage(failure));
        log.error("Application event {} delivery failed on attempt {}; it remains in the outbox for retry",
                entry.getEventId(), attempts, failure);
    }

    private static Instant nextAttempt(ApplicationOutboxEntity entry) {
        long delay = Math.min(MAX_RETRY_DELAY_SECONDS, 1L << Math.min(entry.getAttemptCount() + 1, 8));
        return Instant.now().plusSeconds(delay);
    }

    private static String errorMessage(Exception exception) {
        String message = exception.getMessage();
        String result = message == null ? exception.getClass().getName() : message;
        return result.length() <= MAX_ERROR_LENGTH ? result : result.substring(0, MAX_ERROR_LENGTH);
    }
}
