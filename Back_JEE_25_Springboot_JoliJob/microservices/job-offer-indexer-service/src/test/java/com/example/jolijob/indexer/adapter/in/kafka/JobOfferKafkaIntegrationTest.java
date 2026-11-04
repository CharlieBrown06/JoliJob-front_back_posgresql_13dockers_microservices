package com.example.jolijob.indexer.adapter.in.kafka;

import com.example.jolijob.indexer.JobOfferIndexerApplication;
import com.example.jolijob.indexer.adapter.out.elasticsearch.JobOfferDocument;
import com.example.jolijob.indexer.adapter.out.elasticsearch.JobOfferDocumentRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Real embedded Kafka, listener, use case and index adapter; only the external
 * Elasticsearch repository is mocked. No Elasticsearch server is required.
 */
@SpringBootTest(classes = JobOfferIndexerApplication.class, properties = {
        "spring.data.elasticsearch.repositories.enabled=false",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
@EmbeddedKafka(partitions = 1, topics = "job-offer.events",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JobOfferKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafka;
    @MockitoBean
    private JobOfferDocumentRepository repository;
    @Autowired
    private ConsumerFactory<?, ?> consumerFactory;

    @Test
    void jsonEventsCreateUpdateAndSoftDeleteAnOffer() throws Exception {
        assertThat(consumerFactory.getConfigurationProperties().get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG))
                .isEqualTo(JacksonJsonDeserializer.class);
        JobOfferDocument created = document("Développeur", true);
        JobOfferDocument updated = document("Architecte", true);
        when(repository.findById("offer-42")).thenReturn(Optional.of(created), Optional.of(updated));

        send("CREATED", "Développeur", "2026-10-20T12:34:56.123Z");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> verify(repository).save(created));

        send("UPDATED", "Architecte", "2026-10-21T12:00:00Z");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> verify(repository).save(updated));

        send("DELETED", "Ignored deletion payload", "2026-10-22T12:00:00Z");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                verify(repository).save(document("Architecte", false)));

        var saved = ArgumentCaptor.forClass(JobOfferDocument.class);
        verify(repository, times(3)).save(saved.capture());
        assertThat(saved.getAllValues()).containsExactly(created, updated, document("Architecte", false));
        verify(repository, never()).deleteById(any());
    }

    private void send(String type, String title, String occurredAt) throws Exception {
        String json = """
                {
                  "eventId": "event-%s", "type": "%s", "jobOfferId": "offer-42",
                  "companyId": "company-42", "companyName": "JoliJob", "title": "%s",
                  "description": "Description", "jobCategory": "IT", "specialty": "Java",
                  "contractType": "CDI", "remote": true, "country": "France",
                  "region": "Île-de-France", "department": "75", "city": "Paris",
                  "salaryMin": 40000.50, "salaryMax": 60000.75, "occurredAt": "%s"
                }
                """.formatted(type, type, title, occurredAt);
        kafka.send("job-offer.events", "offer-42", json).get(10, TimeUnit.SECONDS);
    }

    private JobOfferDocument document(String title, boolean active) {
        return new JobOfferDocument("offer-42", "company-42", "JoliJob", title,
                "Description", "IT", "Java", "CDI", true, "France", "Île-de-France",
                "75", "Paris", new BigDecimal("40000.50"), new BigDecimal("60000.75"),
                active, Instant.parse("2026-10-20T12:34:56.123Z"));
    }

}
