package com.example.jolijob.joboffer.adapter.out.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

import static com.example.jolijob.joboffer.OfferFixture.*;
import static com.example.jolijob.joboffer.domain.JobOfferEventType.UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JobOfferEventMessageTest {
    @Test
    void serializesExistingPayloadWithUppercaseStringTypesAndNoJavaTypeHeaders() throws Exception {
        var properties = new YamlPropertySourceLoader().load("service",
                new ClassPathResource("application.yaml")).getFirst();
        Map<String, Object> serializerConfig = new HashMap<>();
        serializerConfig.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS,
                properties.getProperty("spring.kafka.producer.properties.spring.json.add.type.headers"));
        try (var serializer = new JacksonJsonSerializer<JobOfferEventMessage>()) {
            serializer.configure(serializerConfig, false);
            var headers = new org.apache.kafka.common.header.internals.RecordHeaders();
            var message = JobOfferEventMessage.from(offer(), UPDATED, NOW);
            var bytes = serializer.serialize("job-offer.events", headers, message);
            var json = JsonMapper.builder().build().readTree(bytes);
            assertThat(headers.toArray()).isEmpty();
            assertThat(json.properties()).extracting(Map.Entry::getKey).containsExactlyInAnyOrder(
                    "eventId", "type", "jobOfferId", "companyId", "companyName", "title", "description",
                    "jobCategory", "specialty", "contractType", "remote", "country", "region",
                    "department", "city", "salaryMin", "salaryMax", "occurredAt");
            assertThatCode(() -> java.util.UUID.fromString(json.get("eventId").asString())).doesNotThrowAnyException();
            assertThat(json.get("type").asString()).isEqualTo("UPDATED");
            assertThat(json.get("jobOfferId").asString()).isEqualTo(OFFER_ID.toString());
            assertThat(json.get("companyId").asString()).isEqualTo(COMPANY_ID.toString());
            assertThat(json.get("companyName").asString()).isEqualTo(offer().companyName());
            assertThat(json.get("title").asString()).isEqualTo(details().title());
            assertThat(json.get("description").asString()).isEqualTo(details().description());
            assertThat(json.get("jobCategory").asString()).isEqualTo(details().jobCategory());
            assertThat(json.get("specialty").asString()).isEqualTo(details().specialty());
            assertThat(json.get("contractType").asString()).isEqualTo("CDI");
            assertThat(json.get("remote").asBoolean()).isTrue();
            assertThat(json.get("country").asString()).isEqualTo(details().country());
            assertThat(json.get("region").asString()).isEqualTo(details().region());
            assertThat(json.get("department").asString()).isEqualTo(details().department());
            assertThat(json.get("city").asString()).isEqualTo(details().city());
            assertThat(json.get("salaryMin").decimalValue()).isEqualByComparingTo(details().salaryMin());
            assertThat(json.get("salaryMax").decimalValue()).isEqualByComparingTo(details().salaryMax());
            assertThat(json.get("occurredAt").asString()).isEqualTo(NOW.toString());
        }
    }
}
