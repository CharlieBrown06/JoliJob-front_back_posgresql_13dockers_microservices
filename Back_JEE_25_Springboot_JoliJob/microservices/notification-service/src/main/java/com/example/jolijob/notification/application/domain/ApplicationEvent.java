package com.example.jolijob.notification.application.domain;

public record ApplicationEvent(
        ApplicationEventType type,
        String applicationId,
        String jobTitle,
        String candidateEmail,
        String companyEmail,
        String newStatus
) {}
