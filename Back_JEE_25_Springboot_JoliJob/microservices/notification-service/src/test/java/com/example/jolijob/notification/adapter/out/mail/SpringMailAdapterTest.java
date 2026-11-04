package com.example.jolijob.notification.adapter.out.mail;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class SpringMailAdapterTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final SpringMailAdapter adapter = new SpringMailAdapter(sender);

    @Test
    void sendsRecipientSubjectBodyAndConfiguredSender() {
        adapter.send("candidate@example.com", "Candidature envoyée", "Votre candidature est enregistrée.");

        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("candidate@example.com");
        assertThat(message.getValue().getSubject()).isEqualTo("Candidature envoyée");
        assertThat(message.getValue().getText()).isEqualTo("Votre candidature est enregistrée.");
        assertThat(message.getValue().getFrom()).isEqualTo("noreply@jolijob.fr");
        verifyNoMoreInteractions(sender);
    }

    @Test
    void propagatesMailSendFailuresWithoutRetrying() {
        var failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(sender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> adapter.send("candidate@example.com", "Subject", "Body"))
                .isSameAs(failure);
        verify(sender).send(any(SimpleMailMessage.class));
        verifyNoMoreInteractions(sender);
    }
}
