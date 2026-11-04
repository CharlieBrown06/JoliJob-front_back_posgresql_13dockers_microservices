package com.example.jolijob.application.adapter.in.web;

import com.example.jolijob.application.domain.Actor;
import com.example.jolijob.application.port.in.ApplicationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {
    private final ApplicationUseCase applications;

    public ApplicationController(ApplicationUseCase applications) {
        this.applications = applications;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationResponse apply(@Valid @RequestBody ApplicationRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        return ApplicationResponse.from(applications.apply(actor(jwt, "CANDIDATE"), request.jobOfferId(),
                request.coverLetter(), request.cvUrl()));
    }

    @PatchMapping("/{applicationId}/status")
    @PreAuthorize("hasRole('COMPANY')")
    public ApplicationResponse updateStatus(@PathVariable UUID applicationId,
                                            @Valid @RequestBody ApplicationStatusRequest request,
                                            @AuthenticationPrincipal Jwt jwt) {
        return ApplicationResponse.from(applications.updateStatus(actor(jwt, "COMPANY"), applicationId,
                request.status()));
    }

    @PatchMapping("/{applicationId}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationResponse withdraw(@PathVariable UUID applicationId,
                                        @AuthenticationPrincipal Jwt jwt) {
        return ApplicationResponse.from(applications.withdraw(actor(jwt, "CANDIDATE"), applicationId));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyCandidateApplications(@AuthenticationPrincipal Jwt jwt) {
        return applications.getMyCandidateApplications(actor(jwt, "CANDIDATE")).stream()
                .map(ApplicationResponse::from).toList();
    }

    @GetMapping("/offer/{jobOfferId}")
    @PreAuthorize("hasRole('COMPANY')")
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getOfferApplications(@PathVariable UUID jobOfferId,
                                                          @AuthenticationPrincipal Jwt jwt) {
        return applications.getOfferApplications(actor(jwt, "COMPANY"), jobOfferId).stream()
                .map(ApplicationResponse::from).toList();
    }

    private static Actor actor(Jwt jwt, String operationRole) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "JWT subject must be a UUID", exception);
        }
        String email = jwt.getClaimAsString("email");
        if ("CANDIDATE".equals(operationRole)
                && (!Boolean.TRUE.equals(jwt.getClaim("email_verified")) || email == null || email.isBlank())) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Candidate token requires a verified email address");
        }
        return new Actor(userId, email, operationRole);
    }
}
