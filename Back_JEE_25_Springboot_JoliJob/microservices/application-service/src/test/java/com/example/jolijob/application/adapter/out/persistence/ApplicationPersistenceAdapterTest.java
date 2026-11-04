package com.example.jolijob.application.adapter.out.persistence;

import com.example.jolijob.application.domain.Application;
import com.example.jolijob.application.domain.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:application-persistence-adapter;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(ApplicationPersistenceAdapter.class)
class ApplicationPersistenceAdapterTest {
    @Autowired
    private ApplicationPersistenceAdapter adapter;
    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReloadsDomainFieldsAndFiltersQueriesByCandidateAndOffer() {
        UUID candidateId = UUID.randomUUID();
        UUID otherCandidateId = UUID.randomUUID();
        UUID jobOfferId = UUID.randomUUID();
        UUID otherJobOfferId = UUID.randomUUID();
        Application first = adapter.save(application(jobOfferId, candidateId));
        Application second = adapter.save(application(otherJobOfferId, candidateId));
        Application unrelated = adapter.save(application(jobOfferId, otherCandidateId));

        entityManager.flush();
        entityManager.clear();

        assertEquals(first, adapter.findById(first.id()).orElseThrow());
        assertTrue(adapter.findById(UUID.randomUUID()).isEmpty());
        assertEquals(2, adapter.findByCandidateId(candidateId).size());
        assertTrue(adapter.findByCandidateId(candidateId).contains(first));
        assertTrue(adapter.findByCandidateId(candidateId).contains(second));
        assertEquals(2, adapter.findByJobOfferId(jobOfferId).size());
        assertTrue(adapter.findByJobOfferId(jobOfferId).contains(first));
        assertTrue(adapter.findByJobOfferId(jobOfferId).contains(unrelated));
        assertTrue(adapter.existsByJobOfferIdAndCandidateId(jobOfferId, candidateId));
        assertFalse(adapter.existsByJobOfferIdAndCandidateId(otherJobOfferId, otherCandidateId));
    }

    private static Application application(UUID jobOfferId, UUID candidateId) {
        Instant submittedAt = Instant.parse("2026-01-02T03:04:05Z");
        return new Application(null, jobOfferId, "Platform Engineer", candidateId,
                "candidate@example.test", UUID.randomUUID(), "company@example.test",
                "Interested in the role", "https://files.example.test/cv.pdf",
                ApplicationStatus.SUBMITTED, submittedAt, submittedAt);
    }
}
