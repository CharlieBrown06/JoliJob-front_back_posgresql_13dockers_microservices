package com.example.jolijob.notification.adapter.in.kafka;

import com.example.jolijob.notification.NotificationApplication;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Real embedded Kafka, listener, use case and mail adapter; only the external
 * JavaMailSender is mocked. No SMTP server or delivery guarantees are tested.
 */
@SpringBootTest(classes = NotificationApplication.class, properties = {
        "management.health.mail.enabled=false",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
@EmbeddedKafka(partitions = 1, topics = "application.events",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApplicationKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafka;
    @MockitoBean
    private JavaMailSender mailSender;
    @Autowired
    private ConsumerFactory<?, ?> consumerFactory;

    @Test
    void jsonMessagesTriggerAllNotificationTypesThroughRealUseCaseAndMailAdapter() throws Exception {
        assertThat(consumerFactory.getConfigurationProperties().get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG))
                .isEqualTo(JacksonJsonDeserializer.class);
        send("SUBMITTED", "Développeur");
        send("STATUS_CHANGED", "Architecte");
        send("WITHDRAWN", "Analyste");

        var messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                verify(mailSender, times(4)).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class)));
        verify(mailSender, times(4)).send(messages.capture());

        assertThat(messages.getAllValues()).containsExactly(
                mail("candidate@example.com", "Candidature envoyée — Développeur",
                        "Votre candidature pour le poste \"Développeur\" a bien été enregistrée. Bonne chance !"),
                mail("company@example.com", "Nouvelle candidature — Développeur",
                        "Vous avez reçu une nouvelle candidature pour le poste \"Développeur\"."),
                mail("candidate@example.com", "Mise à jour de votre candidature — Architecte",
                        "Le statut de votre candidature pour \"Architecte\" a été mis à jour : ACCEPTED"),
                mail("company@example.com", "Candidature retirée — Analyste",
                        "Un(e) candidat(e) a retiré sa candidature pour le poste \"Analyste\"."));
    }

    private void send(String type, String title) throws Exception {
        String json = """
                {
                  "eventId": "event-%s", "type": "%s", "applicationId": "application-42",
                  "jobOfferId": "offer-42", "jobTitle": "%s", "candidateId": "candidate-42",
                  "candidateEmail": "candidate@example.com", "companyId": "company-42",
                  "companyEmail": "company@example.com", "newStatus": "ACCEPTED",
                  "occurredAt": "2026-10-20T12:34:56.123Z"
                }
                """.formatted(type, type, title);
        kafka.send("application.events", "application-42", json).get(10, TimeUnit.SECONDS);
    }

    private SimpleMailMessage mail(String to, String subject, String body) {
        var message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("noreply@jolijob.fr");
        return message;
    }

}
