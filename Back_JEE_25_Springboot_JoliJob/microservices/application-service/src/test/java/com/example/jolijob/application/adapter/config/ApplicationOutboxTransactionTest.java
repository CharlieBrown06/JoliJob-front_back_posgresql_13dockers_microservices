package com.example.jolijob.application.adapter.config;

import com.example.jolijob.application.domain.Actor;
import com.example.jolijob.application.domain.ApplicationEvent;
import com.example.jolijob.application.domain.ApplicationStatus;
import com.example.jolijob.application.port.in.ApplicationUseCase;
import com.example.jolijob.application.port.out.ApplicationRepositoryPort;
import com.example.jolijob.application.adapter.out.persistence.ApplicationOutboxAdapter;
import com.example.jolijob.application.adapter.out.persistence.SpringDataApplicationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:application-outbox-transaction;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({ApplicationOutboxAdapter.class, ApplicationOutboxTransactionTest.JsonConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ApplicationOutboxTransactionTest {
    @Autowired
    private ApplicationOutboxAdapter eventPort;
    @Autowired
    private SpringDataApplicationOutboxRepository outbox;
    @Autowired
    private PlatformTransactionManager transactionManager;
    private ApplicationRepositoryPort applications = org.mockito.Mockito.mock(ApplicationRepositoryPort.class);
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TransactionTemplate transactions;

    @BeforeEach
    void clearOutbox() {
        transactions = new TransactionTemplate(transactionManager);
        outbox.deleteAll();
        jdbcTemplate.update("delete from applications");
    }

    @Test
    void useCaseCallCommitsOutboxEntryWithoutAnHttpTransaction() {
        ApplicationUseCase delegate = mock(ApplicationUseCase.class);
        ApplicationEvent event = event("SUBMITTED", UUID.randomUUID().toString());
        when(delegate.apply(any(Actor.class), any(UUID.class), any(), nullable(String.class))).thenAnswer(invocation -> {
            writeApplicationRow();
            eventPort.publish(event);
            return null;
        });
        TransactionalApplicationUseCase useCase = new TransactionalApplicationUseCase(
                delegate, applications, transactions);

        useCase.apply(new Actor(UUID.randomUUID(), "candidate@example.test", "CANDIDATE"),
                UUID.randomUUID(), "Cover letter", null);

        assertEquals(1, outbox.count());
        assertEquals(1, jdbcTemplate.queryForObject("select count(*) from applications", Integer.class));
    }

    @Test
    void useCaseFailureRollsBackOutboxEntry() {
        ApplicationUseCase delegate = mock(ApplicationUseCase.class);
        ApplicationEvent event = event("SUBMITTED", UUID.randomUUID().toString());
        when(delegate.apply(any(Actor.class), any(UUID.class), any(), nullable(String.class))).thenAnswer(invocation -> {
            writeApplicationRow();
            eventPort.publish(event);
            throw new IllegalStateException("application write failed");
        });
        TransactionalApplicationUseCase useCase = new TransactionalApplicationUseCase(
                delegate, applications, transactions);

        assertThrows(IllegalStateException.class, () -> useCase.apply(
                new Actor(UUID.randomUUID(), "candidate@example.test", "CANDIDATE"),
                UUID.randomUUID(), "Cover letter", null));

        assertEquals(0, outbox.count());
        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from applications", Integer.class));
    }

    @Test
    void existingAggregateIsLockedBeforeStatusMutation() {
        ApplicationUseCase delegate = mock(ApplicationUseCase.class);
        ApplicationRepositoryPort applicationRepository = mock(ApplicationRepositoryPort.class);
        UUID applicationId = UUID.randomUUID();
        TransactionalApplicationUseCase useCase = new TransactionalApplicationUseCase(
                delegate, applicationRepository, transactions);

        useCase.updateStatus(new Actor(UUID.randomUUID(), "company@example.test", "COMPANY"),
                applicationId, ApplicationStatus.ACCEPTED);

        var ordered = inOrder(applicationRepository, delegate);
        ordered.verify(applicationRepository).lockById(applicationId);
        ordered.verify(delegate).updateStatus(any(Actor.class),
                org.mockito.ArgumentMatchers.eq(applicationId),
                org.mockito.ArgumentMatchers.eq(ApplicationStatus.ACCEPTED));
    }

    @Test
    void existingAggregateIsLockedBeforeWithdrawMutation() {
        ApplicationUseCase delegate = mock(ApplicationUseCase.class);
        ApplicationRepositoryPort applicationRepository = mock(ApplicationRepositoryPort.class);
        UUID applicationId = UUID.randomUUID();
        TransactionalApplicationUseCase useCase = new TransactionalApplicationUseCase(
                delegate, applicationRepository, transactions);

        useCase.withdraw(new Actor(UUID.randomUUID(), "candidate@example.test", "CANDIDATE"), applicationId);

        var ordered = inOrder(applicationRepository, delegate);
        ordered.verify(applicationRepository).lockById(applicationId);
        ordered.verify(delegate).withdraw(any(Actor.class), org.mockito.ArgumentMatchers.eq(applicationId));
    }

    private static ApplicationEvent event(String type, String aggregateId) {
        return new ApplicationEvent(UUID.randomUUID().toString(), type, aggregateId,
                UUID.randomUUID().toString(), "Developer", UUID.randomUUID().toString(),
                "candidate@example.test", UUID.randomUUID().toString(), "company@example.test",
                ApplicationStatus.SUBMITTED.name(), Instant.now().toString());
    }

    private void writeApplicationRow() {
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                insert into applications (id, job_offer_id, candidate_id, status, submitted_at, updated_at)
                values (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ApplicationStatus.SUBMITTED.name(), now, now);
    }

    @TestConfiguration
    static class JsonConfiguration {
        @Bean
        ObjectMapper outboxObjectMapper() {
            return JsonMapper.builder().build();
        }
    }
}
