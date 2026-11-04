package com.example.jolijob.shared.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic jobOfferEventsTopic() {
        return TopicBuilder.name(KafkaTopics.JOB_OFFER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic applicationEventsTopic() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_EVENTS).partitions(3).replicas(1).build();
    }
}
