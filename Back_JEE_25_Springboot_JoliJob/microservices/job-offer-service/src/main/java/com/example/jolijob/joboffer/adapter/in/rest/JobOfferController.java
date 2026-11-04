package com.example.jolijob.joboffer.adapter.in.rest;

import com.example.jolijob.joboffer.application.port.in.JobOfferUseCases;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/job-offers")
public class JobOfferController {
    private final JobOfferUseCases offers;

    public JobOfferController(JobOfferUseCases offers) {
        this.offers = offers;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPANY')")
    public JobOfferResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody JobOfferRequest request) {
        return JobOfferResponse.from(offers.create(userId(jwt), request.toDetails()));
    }

    @PutMapping("/{offerId}")
    @PreAuthorize("hasRole('COMPANY')")
    public JobOfferResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID offerId,
                                   @Valid @RequestBody JobOfferRequest request) {
        return JobOfferResponse.from(offers.update(userId(jwt), offerId, request.toDetails()));
    }

    @DeleteMapping("/{offerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('COMPANY')")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID offerId) {
        offers.delete(userId(jwt), offerId);
    }

    @GetMapping("/{offerId}")
    @Transactional(readOnly = true)
    public JobOfferResponse getById(@PathVariable UUID offerId) {
        return JobOfferResponse.from(offers.getById(offerId));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('COMPANY')")
    @Transactional(readOnly = true)
    public List<JobOfferResponse> getMyOffers(@AuthenticationPrincipal Jwt jwt) {
        return offers.getMyOffers(userId(jwt)).stream().map(JobOfferResponse::from).toList();
    }

    private static UUID userId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("JWT subject must be a UUID", ex);
        } catch (NullPointerException ex) {
            throw new BadCredentialsException("JWT subject is required", ex);
        }
    }
}
