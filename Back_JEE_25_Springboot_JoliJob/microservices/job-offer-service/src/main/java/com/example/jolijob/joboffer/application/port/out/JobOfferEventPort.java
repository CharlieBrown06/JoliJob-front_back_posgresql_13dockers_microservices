package com.example.jolijob.joboffer.application.port.out;

import com.example.jolijob.joboffer.domain.JobOffer;
import com.example.jolijob.joboffer.domain.JobOfferEventType;

public interface JobOfferEventPort {
    void publish(JobOffer offer, JobOfferEventType type);
}
