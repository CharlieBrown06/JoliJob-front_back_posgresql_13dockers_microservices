package com.example.jolijob.application.adapter.config;

import com.example.jolijob.application.adapter.out.kafka.ApplicationOutboxRelay;
import com.example.jolijob.application.adapter.out.persistence.ApplicationOutboxAdapter;
import com.example.jolijob.application.adapter.out.persistence.ApplicationOutboxEntity;
import com.example.jolijob.application.adapter.out.persistence.SpringDataApplicationOutboxRepository;
import com.example.jolijob.application.domain.ApplicationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:application-outbox-relay;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "application.outbox.batch-size=2"
})
@Import({ApplicationOutboxAdapter.class, ApplicationOutboxRelay.class, ApplicationOutboxRelayTest.JsonConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ApplicationOutboxRelayTest {
    private static final String TOPIC = "application.events";

    @Autowired
    private ApplicationOutboxAdapter eventPort;
    @Autowired
    private SpringDataApplicationOutboxRepository outbox;
    @Autowired
    private ApplicationOutboxRelay relay;
    @Autowired
    private KafkaTemplate<String, ApplicationEvent> kafkaTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetDatabaseAndKafka() {
        reset(kafkaTemplate);
        outbox.deleteAll();
    }

    @Test
    void failedEventIsRetainedAndRetriedBeforeLaterEventsForTheSameApplication() throws Exception {
        String aggregate = UUID.randomUUID().toString();
        ApplicationEvent first = event(aggregate, "SUBMITTED");
        ApplicationEvent second = event(aggregate, "STATUS_CHANGED");
        ApplicationEvent independent = event(UUID.randomUUID().toString(), "SUBMITTED");
        ApplicationEvent independentBeyondBatch = event(UUID.randomUUID().toString(), "SUBMITTED");
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> {
            eventPort.publish(first);
            eventPort.publish(second);
            eventPort.publish(independent);
            eventPort.publish(independentBeyondBatch);
        });

        List<ApplicationEvent> sends = new ArrayList<>();
        doAnswer(invocation -> {
            ApplicationEvent event = invocation.getArgument(2);
            sends.add(event);
            if (event.eventId().equals(first.eventId()) && sends.size() == 1) {
                return CompletableFuture.<SendResult<String, ApplicationEvent>>failedFuture(
                        new IllegalStateException("broker unavailable"));
            }
            return CompletableFuture.<SendResult<String, ApplicationEvent>>completedFuture(null);
        }).when(kafkaTemplate).send(eq(TOPIC), anyString(), any(ApplicationEvent.class));

        relay.relay();

        assertEquals(List.of(first.eventId(), independent.eventId()),
                sends.stream().map(ApplicationEvent::eventId).toList());
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from application_outbox where attempt_count = 1 and last_error is not null",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from application_outbox where published_at is not null", Integer.class));

        jdbcTemplate.update("update application_outbox set next_attempt_at = ? where event_id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)), first.eventId());
        relay.relay();
        relay.relay();

        assertEquals(List.of(first.eventId(), independent.eventId(), first.eventId(),
                        independentBeyondBatch.eventId(), second.eventId()),
                sends.stream().map(ApplicationEvent::eventId).toList());
        assertEquals(4, jdbcTemplate.queryForObject(
                "select count(*) from application_outbox where published_at is not null", Integer.class));
    }

    @Test
    void concurrentRelayCannotSkipLockedAggregateHead() throws Exception {
        String aggregate = UUID.randomUUID().toString();
        ApplicationEvent first = event(aggregate, "SUBMITTED");
        ApplicationEvent second = event(aggregate, "STATUS_CHANGED");
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> {
            eventPort.publish(first);
            eventPort.publish(second);
        });

        CountDownLatch headLocked = new CountDownLatch(1);
        CountDownLatch releaseHead = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            var firstClaim = executor.submit(() -> transactions.execute(status -> {
                List<ApplicationOutboxEntity> claimed = outbox.claimNextBatch(Instant.now(), 10);
                headLocked.countDown();
                try {
                    if (!releaseHead.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to release claimed outbox head");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while holding outbox head", exception);
                }
                return claimed.stream().map(ApplicationOutboxEntity::getEventId).toList();
            }));
            assertTrue(headLocked.await(5, TimeUnit.SECONDS));

            List<ApplicationOutboxEntity> secondClaim =
                    transactions.execute(status -> outbox.claimNextBatch(Instant.now(), 10));
            assertEquals(List.of(), secondClaim);

            releaseHead.countDown();
            assertEquals(List.of(first.eventId()), firstClaim.get(5, TimeUnit.SECONDS));
            List<ApplicationOutboxEntity> afterRelease =
                    transactions.execute(status -> outbox.claimNextBatch(Instant.now(), 10));
            assertEquals(List.of(first.eventId()),
                    afterRelease.stream().map(ApplicationOutboxEntity::getEventId).toList());
        } finally {
            releaseHead.countDown();
            executor.shutdownNow();
        }
    }

    private static ApplicationEvent event(String aggregateId, String type) {
        return new ApplicationEvent(UUID.randomUUID().toString(), type, aggregateId,
                UUID.randomUUID().toString(), "Developer", UUID.randomUUID().toString(),
                "candidate@example.test", UUID.randomUUID().toString(), "company@example.test",
                "SUBMITTED", Instant.now().toString());
    }

    @TestConfiguration
    static class JsonConfiguration {
        @Bean
        ObjectMapper outboxObjectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, ApplicationEvent> applicationEventsKafkaTemplate() {
            return org.mockito.Mockito.mock(KafkaTemplate.class);
        }
    }
}
