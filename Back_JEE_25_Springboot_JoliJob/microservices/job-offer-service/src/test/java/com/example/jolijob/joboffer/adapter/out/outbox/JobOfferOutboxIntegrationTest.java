package com.example.jolijob.joboffer.adapter.out.outbox;

import com.example.jolijob.joboffer.application.port.in.JobOfferUseCases;
import com.example.jolijob.joboffer.application.port.out.CompanyPort;
import com.example.jolijob.joboffer.adapter.out.kafka.JobOfferEventMessage;
import com.example.jolijob.joboffer.domain.JobOfferEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

import static com.example.jolijob.joboffer.OfferFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:job-offer-outbox;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "job-offer.outbox.relay.enabled=false",
        "job-offer.outbox.relay.ack-timeout=100ms"
})
class JobOfferOutboxIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired JobOfferUseCases offers;
    @Autowired JobOfferOutboxAdapter outbox;
    @Autowired JobOfferOutboxBatchProcessor processor;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean CompanyPort companies;
    @MockitoBean KafkaTemplate<String, Object> kafka;

    @BeforeEach
    void cleanDatabaseAndConfigureAdapters() {
        jdbc.update("DELETE FROM job_offer_outbox");
        jdbc.update("DELETE FROM job_offers");
        when(companies.findByUserId(USER_ID)).thenReturn(Optional.of(
                new CompanyPort.Company(COMPANY_ID, USER_ID, "Joli Company")));
        when(kafka.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @AfterEach
    void removeTestConstraint() {
        jdbc.execute("ALTER TABLE job_offer_outbox DROP CONSTRAINT IF EXISTS reject_outbox_events");
    }

    @Test
    void offerAndOutboxCommitAtomicallyAndRollbackTogether() {
        assertThat(AopUtils.isAopProxy(offers)).isTrue();
        var created = offers.create(USER_ID, details());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_offers WHERE id = ?", Integer.class,
                created.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_offer_outbox WHERE aggregate_id = ?",
                Integer.class, created.id())).isEqualTo(1);

        var transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            offers.create(USER_ID, details());
            status.setRollbackOnly();
        });

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_offers", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_offer_outbox", Integer.class)).isEqualTo(1);
    }

    @Test
    void failedOutboxAppendRollsBackOfferEvenWhenCalledWithoutAnOuterTransaction() {
        jdbc.execute("""
                ALTER TABLE job_offer_outbox
                ADD CONSTRAINT reject_outbox_events CHECK (topic <> 'job-offer.events')
                """);

        assertThatThrownBy(() -> offers.create(USER_ID, details()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_offers", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_offer_outbox", Integer.class)).isZero();
    }

    @Test
    void retainsFailedEventsAndRetriesTheSameStablePayload() {
        var created = offers.create(USER_ID, details());
        when(kafka.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertThatThrownBy(() -> processor.publishBatch(10)).hasRootCauseMessage("Kafka unavailable");
        assertThat(pendingCount(created.id())).isEqualTo(1);

        assertThat(processor.publishBatch(10)).isEqualTo(1);
        assertThat(pendingCount(created.id())).isZero();
        var messages = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(kafka, times(2)).send(eq(JobOfferOutboxRelay.TOPIC), eq(created.id().toString()),
                messages.capture());
        assertThat(messages.getAllValues())
                .extracting(message -> ((JobOfferEventMessage) message).eventId())
                .containsOnly(((JobOfferEventMessage) messages.getAllValues().getFirst()).eventId());
    }

    @Test
    void timedOutAcknowledgementLeavesEventForRetry() {
        var created = offers.create(USER_ID, details());
        when(kafka.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<>());

        assertThatThrownBy(() -> processor.publishBatch(10))
                .hasRootCauseInstanceOf(TimeoutException.class);
        assertThat(pendingCount(created.id())).isEqualTo(1);
    }

    @Test
    void interruptionRestoresThreadFlagAndRetainsEvent() {
        var created = offers.create(USER_ID, details());
        when(kafka.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>() {
            @Override
            public SendResult<String, Object> get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                throw new InterruptedException("test interruption");
            }
        });

        try {
            assertThatThrownBy(() -> processor.publishBatch(10))
                    .hasRootCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(pendingCount(created.id())).isEqualTo(1);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void publishesEventsInAggregateSequenceAndOnlyAdvancesOneAtATime() {
        var created = offers.create(USER_ID, details());
        offers.update(USER_ID, created.id(), details());
        offers.delete(USER_ID, created.id());

        assertThat(processor.publishBatch(10)).isEqualTo(1);
        assertThat(processor.publishBatch(10)).isEqualTo(1);
        assertThat(processor.publishBatch(10)).isEqualTo(1);
        assertThat(processor.publishBatch(10)).isZero();

        var events = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(kafka, times(3)).send(eq(JobOfferOutboxRelay.TOPIC), eq(created.id().toString()), events.capture());
        assertThat(events.getAllValues())
                .extracting(message -> ((com.example.jolijob.joboffer.adapter.out.kafka.JobOfferEventMessage) message)
                        .type())
                .containsExactly(JobOfferEventType.CREATED.name(), JobOfferEventType.UPDATED.name(),
                        JobOfferEventType.DELETED.name());
        assertThat(pendingCount(created.id())).isZero();
    }

    @Test
    void serializesConcurrentWritesBeforeAllocatingAggregateEventSequence() throws Exception {
        var created = offers.create(USER_ID, details());
        var firstHasWritten = new CountDownLatch(1);
        var allowFirstCommit = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                offers.update(USER_ID, created.id(), detailsWithTitle("first committed update"));
                firstHasWritten.countDown();
                await(allowFirstCommit);
                return null;
            }));

            assertThat(firstHasWritten.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                secondStarted.countDown();
                return offers.update(USER_ID, created.id(), detailsWithTitle("second committed update"));
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            allowFirstCommit.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            allowFirstCommit.countDown();
        }

        var payloads = jdbc.queryForList("""
                SELECT payload FROM job_offer_outbox
                WHERE aggregate_id = ? AND event_order > (
                    SELECT MIN(event_order) FROM job_offer_outbox WHERE aggregate_id = ?
                )
                ORDER BY event_order
                """, String.class, created.id(), created.id());
        assertThat(payloads).hasSize(2);
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        assertThat(payloads).extracting(payload -> json.readTree(payload).get("title").asString())
                .containsExactly("first committed update", "second committed update");
    }

    private static com.example.jolijob.joboffer.domain.JobOfferDetails detailsWithTitle(String title) {
        var details = details();
        return new com.example.jolijob.joboffer.domain.JobOfferDetails(title, details.description(),
                details.jobCategory(), details.specialty(), details.contractType(), details.remote(),
                details.country(), details.region(), details.department(), details.city(), details.salaryMin(),
                details.salaryMax(), details.startDate(), details.expiresAt());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for transaction release");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for transaction release", exception);
        }
    }

    private int pendingCount(java.util.UUID aggregateId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM job_offer_outbox
                WHERE aggregate_id = ? AND published_at IS NULL
                """, Integer.class, aggregateId);
    }
}
