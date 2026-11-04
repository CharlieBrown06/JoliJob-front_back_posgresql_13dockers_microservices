package com.example.jolijob.indexer.application.port.in;

import com.example.jolijob.indexer.application.domain.JobOfferEvent;

public interface ProcessJobOfferEvent {
    void process(JobOfferEvent event);
}
