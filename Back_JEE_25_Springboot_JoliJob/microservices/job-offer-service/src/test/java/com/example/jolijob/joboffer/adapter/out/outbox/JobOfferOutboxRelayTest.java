package com.example.jolijob.joboffer.adapter.out.outbox;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JobOfferOutboxRelayTest {
    @Test
    void backsOffWithCapAndResetsAfterSuccessfulBatch() {
        var processor = mock(JobOfferOutboxBatchProcessor.class);
        var clock = new MutableClock(Instant.parse("2026-10-21T00:00:00Z"));
        var relay = new JobOfferOutboxRelay(processor, clock, Duration.ofSeconds(2), Duration.ofSeconds(5));
        when(processor.publishBatch(100))
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenReturn(0);

        relay.relay();
        assertThat(relay.consecutiveFailures()).isEqualTo(1);
        assertThat(relay.retryAfter()).isEqualTo(clock.instant().plusSeconds(2));

        relay.relay();
        verify(processor, times(1)).publishBatch(100);
        clock.advance(Duration.ofSeconds(2));
        relay.relay();
        assertThat(relay.consecutiveFailures()).isEqualTo(2);
        assertThat(relay.retryAfter()).isEqualTo(clock.instant().plusSeconds(4));

        clock.advance(Duration.ofSeconds(4));
        relay.relay();
        assertThat(relay.consecutiveFailures()).isEqualTo(3);
        assertThat(relay.retryAfter()).isEqualTo(clock.instant().plusSeconds(5));

        clock.advance(Duration.ofSeconds(5));
        relay.relay();
        assertThat(relay.consecutiveFailures()).isZero();
        assertThat(relay.retryAfter()).isEqualTo(Instant.MIN);
        verify(processor, times(4)).publishBatch(100);
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return instant.get();
        }

        private void advance(Duration duration) {
            instant.updateAndGet(current -> current.plus(duration));
        }
    }
}
