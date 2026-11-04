package com.example.jolijob.application.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SpringDataApplicationOutboxRepository extends JpaRepository<ApplicationOutboxEntity, Long> {
    @Query(value = """
            SELECT o.*
            FROM application_outbox o
            WHERE o.published_at IS NULL
              AND o.next_attempt_at <= :now
              AND NOT EXISTS (
                  SELECT 1
                  FROM application_outbox earlier
                  WHERE earlier.aggregate_id = o.aggregate_id
                    AND earlier.published_at IS NULL
                    AND earlier.sequence_id < o.sequence_id
              )
            ORDER BY o.sequence_id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<ApplicationOutboxEntity> claimNextBatch(@Param("now") Instant now,
                                                  @Param("batchSize") int batchSize);
}
