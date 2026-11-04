package com.example.jolijob;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.main.lazy-initialization=true",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.modulith.events.jpa.JpaEventPublicationAutoConfiguration," +
                "org.springframework.modulith.events.config.EventPublicationAutoConfiguration," +
                "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration," +
                "org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration," +
                "org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchRepositoriesAutoConfiguration," +
                "org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchReactiveRepositoriesAutoConfiguration," +
                "org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration," +
                "org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration"
})
class JoliJobApplicationTests {

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void contextLoads() {
    }
}
