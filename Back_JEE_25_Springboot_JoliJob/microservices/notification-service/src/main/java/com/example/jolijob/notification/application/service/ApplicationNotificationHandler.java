package com.example.jolijob.notification.application.service;

import com.example.jolijob.notification.application.domain.ApplicationEvent;
import com.example.jolijob.notification.application.port.in.ProcessApplicationEvent;
import com.example.jolijob.notification.application.port.out.EmailSenderPort;

public class ApplicationNotificationHandler implements ProcessApplicationEvent {

    private final EmailSenderPort emailSender;

    public ApplicationNotificationHandler(EmailSenderPort emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void process(ApplicationEvent event) {
        switch (event.type()) {
            case SUBMITTED -> notifySubmitted(event);
            case STATUS_CHANGED -> notifyStatusChanged(event);
            case WITHDRAWN -> notifyWithdrawn(event);
        }
    }

    private void notifySubmitted(ApplicationEvent event) {
        sendIfPresent(event.candidateEmail(),
                "Candidature envoyée — " + event.jobTitle(),
                "Votre candidature pour le poste \"" + event.jobTitle() +
                        "\" a bien été enregistrée. Bonne chance !");
        sendIfPresent(event.companyEmail(),
                "Nouvelle candidature — " + event.jobTitle(),
                "Vous avez reçu une nouvelle candidature pour le poste \"" + event.jobTitle() + "\".");
    }

    private void notifyStatusChanged(ApplicationEvent event) {
        sendIfPresent(event.candidateEmail(),
                "Mise à jour de votre candidature — " + event.jobTitle(),
                "Le statut de votre candidature pour \"" + event.jobTitle() +
                        "\" a été mis à jour : " + event.newStatus());
    }

    private void notifyWithdrawn(ApplicationEvent event) {
        sendIfPresent(event.companyEmail(),
                "Candidature retirée — " + event.jobTitle(),
                "Un(e) candidat(e) a retiré sa candidature pour le poste \"" + event.jobTitle() + "\".");
    }

    private void sendIfPresent(String recipient, String subject, String body) {
        if (recipient != null && !recipient.isBlank()) {
            emailSender.send(recipient, subject, body);
        }
    }
}
