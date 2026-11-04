package com.example.jolijob.joboffer.adapter.in.rest;

import com.example.jolijob.joboffer.application.port.in.JobOfferUseCases;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/job-offers")
public class InternalJobOfferController {
    private final JobOfferUseCases offers;

    public InternalJobOfferController(JobOfferUseCases offers) {
        this.offers = offers;
    }

    @GetMapping("/{id}")
    public JobOfferSummary getById(@PathVariable UUID id) {
        var offer = offers.getById(id);
        return new JobOfferSummary(offer.id(), offer.active(), offer.companyId(), offer.details().title());
    }

    public record JobOfferSummary(UUID id, boolean active, UUID companyId, String title) {}
}
