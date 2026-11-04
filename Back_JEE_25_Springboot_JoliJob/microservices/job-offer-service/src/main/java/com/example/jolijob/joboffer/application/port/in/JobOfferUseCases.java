package com.example.jolijob.joboffer.application.port.in;

import com.example.jolijob.joboffer.domain.JobOffer;
import com.example.jolijob.joboffer.domain.JobOfferDetails;

import java.util.List;
import java.util.UUID;

public interface JobOfferUseCases {
    JobOffer create(UUID userId, JobOfferDetails details);
    JobOffer update(UUID userId, UUID offerId, JobOfferDetails details);
    void delete(UUID userId, UUID offerId);
    JobOffer getById(UUID offerId);
    List<JobOffer> getMyOffers(UUID userId);
}
