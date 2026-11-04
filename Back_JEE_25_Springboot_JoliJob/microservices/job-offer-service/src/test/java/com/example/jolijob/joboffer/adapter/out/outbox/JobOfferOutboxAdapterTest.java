package com.example.jolijob.joboffer.adapter.out.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.example.jolijob.joboffer.OfferFixture.*;
import static com.example.jolijob.joboffer.domain.JobOfferEventType.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class JobOfferOutboxAdapterTest {
    @Test
    void appendsStableEventIdentityAndExistingJsonPayloadToOutbox() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var adapter = new JobOfferOutboxAdapter(jdbc, JsonMapper.builder().build(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        adapter.publish(offer(), CREATED);

        var arguments = org.mockito.ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("INSERT INTO job_offer_outbox"), arguments.capture());
        Object[] values = arguments.getValue();
        var eventId = (UUID) values[0];
        assertThat(values[1]).isEqualTo(OFFER_ID);
        assertThat(values[2]).isEqualTo(JobOfferOutboxRelay.TOPIC);
        assertThat(values[4]).isEqualTo(Timestamp.from(NOW));

        var payload = JsonMapper.builder().build().readTree((String) values[3]);
        assertThat(payload.get("eventId").asString()).isEqualTo(eventId.toString());
        assertThat(payload.get("type").asString()).isEqualTo("CREATED");
        assertThat(payload.get("jobOfferId").asString()).isEqualTo(OFFER_ID.toString());
        assertThat(payload.get("occurredAt").asString()).isEqualTo(NOW.toString());
    }
}
