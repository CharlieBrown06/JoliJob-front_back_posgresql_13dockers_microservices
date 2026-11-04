package com.example.jolijob.application.port.in;

import com.example.jolijob.application.domain.Actor;
import com.example.jolijob.application.domain.Application;
import com.example.jolijob.application.domain.ApplicationStatus;

import java.util.List;
import java.util.UUID;

public interface ApplicationUseCase {
    Application apply(Actor actor, UUID jobOfferId, String coverLetter, String cvUrl);

    Application updateStatus(Actor actor, UUID applicationId, ApplicationStatus status);

    Application withdraw(Actor actor, UUID applicationId);

    List<Application> getMyCandidateApplications(Actor actor);

    List<Application> getOfferApplications(Actor actor, UUID jobOfferId);
}
