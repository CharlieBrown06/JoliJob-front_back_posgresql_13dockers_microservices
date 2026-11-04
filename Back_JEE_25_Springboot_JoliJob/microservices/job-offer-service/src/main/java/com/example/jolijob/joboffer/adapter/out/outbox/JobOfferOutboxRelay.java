package com.example.jolijob.joboffer.adapter.out.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "job-offer.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class JobOfferOutboxRelay {
    public static final String TOPIC = "job-offer.events";
    private static final int BATCH_SIZE = 100;
    private static final Logger logger = LoggerFactory.getLogger(JobOfferOutboxRelay.class);

    private final JobOfferOutboxBatchProcessor processor;
    private final Clock clock;
    private final Duration retryBackoff;
    private final Duration maxRetryBackoff;
    private int consecutiveFailures;
    private Instant retryAfter = Instant.MIN;

    public JobOfferOutboxRelay(JobOfferOutboxBatchProcessor processor, Clock clock,
                               @Value("${job-offer.outbox.relay.retry-backoff:1s}") Duration retryBackoff,
                               @Value("${job-offer.outbox.relay.max-retry-backoff:1m}") Duration maxRetryBackoff) {
        this.processor = processor;
        this.clock = clock;
        if (retryBackoff.isNegative() || retryBackoff.isZero()
                || maxRetryBackoff.compareTo(retryBackoff) < 0) {
            throw new IllegalArgumentException("Retry backoff must be positive and no greater than its maximum");
        }
        this.retryBackoff = retryBackoff;
        this.maxRetryBackoff = maxRetryBackoff;
    }

    @Scheduled(fixedDelayString = "${job-offer.outbox.relay.fixed-delay:1000}")
    public void relay() {
        if (clock.instant().isBefore(retryAfter)) {
            return;
        }
        try {
            processor.publishBatch(BATCH_SIZE);
            consecutiveFailures = 0;
            retryAfter = Instant.MIN;
        } catch (RuntimeException exception) {
            consecutiveFailures = Math.min(consecutiveFailures + 1, 31);
            long multiplier = 1L << (consecutiveFailures - 1);
            long baseMillis = retryBackoff.toMillis();
            long maxMillis = maxRetryBackoff.toMillis();
            long delayMillis = baseMillis >= maxMillis / multiplier
                    ? maxMillis : Math.min(baseMillis * multiplier, maxMillis);
            retryAfter = clock.instant().plusMillis(delayMillis);
            logger.error("Outbox relay failed; retry {} scheduled after {} ({} consecutive failures)",
                    retryAfter, Duration.ofMillis(delayMillis), consecutiveFailures, exception);
        }
    }

    int consecutiveFailures() {
        return consecutiveFailures;
    }

    Instant retryAfter() {
        return retryAfter;
    }
}
