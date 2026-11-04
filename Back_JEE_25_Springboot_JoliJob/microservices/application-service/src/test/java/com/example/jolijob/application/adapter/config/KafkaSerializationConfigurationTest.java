package com.example.jolijob.application.adapter.config;

import com.example.jolijob.application.domain.ApplicationEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaSerializationConfigurationTest {
    @Test
    void productionConfigurationUsesJackson3AndProducesPortableJsonWithoutJavaTypeHeaders() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var configuration = context.getBean(ProducerFactory.class).getConfigurationProperties();
                    assertThat(configuration.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG))
                            .isEqualTo(JacksonJsonSerializer.class);
                    var event = new ApplicationEvent("event-1", "SUBMITTED", "application-1", "offer-1",
                            "Java developer", "candidate-1", "candidate@example.test", "company-1",
                            "company@example.test", "SUBMITTED", "2026-10-20T12:00:00Z");
                    var headers = new RecordHeaders();
                    try (var serializer = new JacksonJsonSerializer<ApplicationEvent>()) {
                        serializer.configure(configuration, false);
                        byte[] bytes = serializer.serialize("application.events", headers, event);

                        var json = JsonMapper.builder().build().readTree(bytes);
                        assertThat(json.path("eventId").asText()).isEqualTo(event.eventId());
                        assertThat(json.path("applicationId").asText()).isEqualTo(event.applicationId());
                        assertThat(json.path("type").asText()).isEqualTo("SUBMITTED");
                        assertThat(json.path("occurredAt").asText()).isEqualTo(event.occurredAt());
                        assertThat(headers.toArray()).isEmpty();
                    }
                });
    }
}
