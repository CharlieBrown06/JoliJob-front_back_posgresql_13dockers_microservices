package com.example.jolijob.joboffer.web;

import com.example.jolijob.joboffer.service.JobOfferService;
import com.example.jolijob.joboffer.web.dto.JobOfferRequest;
import com.example.jolijob.joboffer.web.dto.JobOfferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/job-offers")
@RequiredArgsConstructor
@Tag(name = "Offres d'emploi", description = "Publication et gestion des offres")
@SecurityRequirement(name = "bearerAuth")
public class JobOfferController {

    private final JobOfferService jobOfferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Publier une offre d'emploi")
    public JobOfferResponse create(@Valid @RequestBody JobOfferRequest request) {
        return jobOfferService.create(request);
    }

    @PutMapping("/{offerId}")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Modifier une offre")
    public JobOfferResponse update(@PathVariable UUID offerId, @Valid @RequestBody JobOfferRequest request) {
        return jobOfferService.update(offerId, request);
    }

    @DeleteMapping("/{offerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Supprimer (désactiver) une offre")
    public void delete(@PathVariable UUID offerId) {
        jobOfferService.delete(offerId);
    }

    @GetMapping("/{offerId}")
    @Operation(summary = "Détail d'une offre (public)")
    public JobOfferResponse getById(@PathVariable UUID offerId) {
        return jobOfferService.getById(offerId);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Mes offres publiées")
    public List<JobOfferResponse> getMyOffers() {
        return jobOfferService.getMyOffers();
    }
}
