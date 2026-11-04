package com.example.jolijob.notification.adapter.in.kafka;

import com.example.jolijob.notification.application.domain.ApplicationEvent;
import com.example.jolijob.notification.application.domain.ApplicationEventType;
import com.example.jolijob.notification.application.port.in.ProcessApplicationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ApplicationKafkaConsumerTest {

    private final ProcessApplicationEvent useCase = mock(ProcessApplicationEvent.class);
    private final ApplicationKafkaConsumer consumer = new ApplicationKafkaConsumer(useCase);

    @ParameterizedTest
    @EnumSource(ApplicationEventType.class)
    void mapsEverySupportedTypeAndApplicationFields(ApplicationEventType type) {
        consumer.consume(message(type.name()));

        verify(useCase).process(new ApplicationEvent(type, "application-42", "Développeur Java",
                "candidate@example.com", "company@example.com", "ACCEPTED"));
    }

    @Test
    void rejectsUnknownTypesBeforeCallingUseCase() {
        assertThatThrownBy(() -> consumer.consume(message("UNKNOWN")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(useCase);
    }

    private ApplicationEventMessage message(String type) {
        return new ApplicationEventMessage("event-42", type, "application-42", "offer-42",
                "Développeur Java", "candidate-42", "candidate@example.com", "company-42",
                "company@example.com", "ACCEPTED", "2026-10-20T12:34:56.123Z");
    }
}
