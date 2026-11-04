package com.example.jolijob.notification.application.service;

import com.example.jolijob.notification.application.domain.ApplicationEvent;
import com.example.jolijob.notification.application.domain.ApplicationEventType;
import com.example.jolijob.notification.application.port.out.EmailSenderPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;

class ApplicationNotificationHandlerTest {

    private final EmailSenderPort emailSender = mock(EmailSenderPort.class);
    private final ApplicationNotificationHandler handler = new ApplicationNotificationHandler(emailSender);

    @Test
    void submittedApplicationNotifiesCandidateAndCompany() {
        handler.process(event("SUBMITTED", "candidate@example.com", "company@example.com", null));

        verify(emailSender).send(
                "candidate@example.com",
                "Candidature envoyée — Développeur",
                "Votre candidature pour le poste \"Développeur\" a bien été enregistrée. Bonne chance !");
        verify(emailSender).send(
                "company@example.com",
                "Nouvelle candidature — Développeur",
                "Vous avez reçu une nouvelle candidature pour le poste \"Développeur\".");
        verifyNoMoreInteractions(emailSender);
    }

    @Test
    void statusChangeOnlyNotifiesCandidate() {
        handler.process(event("STATUS_CHANGED", "candidate@example.com", "company@example.com", "VIEWED"));

        verify(emailSender).send(
                "candidate@example.com",
                "Mise à jour de votre candidature — Développeur",
                "Le statut de votre candidature pour \"Développeur\" a été mis à jour : VIEWED");
        verifyNoMoreInteractions(emailSender);
    }

    @Test
    void withdrawalOnlyNotifiesCompany() {
        handler.process(event("WITHDRAWN", "candidate@example.com", "company@example.com", null));

        verify(emailSender).send("company@example.com", "Candidature retirée — Développeur",
                "Un(e) candidat(e) a retiré sa candidature pour le poste \"Développeur\".");
        verifyNoMoreInteractions(emailSender);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void missingRecipientsAreSkippedForEveryEventType(String recipient) {
        for (ApplicationEventType type : ApplicationEventType.values()) {
            handler.process(event(type.name(), recipient, recipient, "VIEWED"));
        }
        verifyNoInteractions(emailSender);
    }

    @Test
    void missingCandidateDoesNotSuppressCompanyNotification() {
        handler.process(event("SUBMITTED", null, "company@example.com", null));

        verify(emailSender).send("company@example.com", "Nouvelle candidature — Développeur",
                "Vous avez reçu une nouvelle candidature pour le poste \"Développeur\".");
        verifyNoMoreInteractions(emailSender);
    }

    private ApplicationEvent event(String type, String candidateEmail, String companyEmail, String status) {
        return new ApplicationEvent(
                ApplicationEventType.valueOf(type),
                "application-1",
                "Développeur",
                candidateEmail,
                companyEmail,
                status);
    }
}
