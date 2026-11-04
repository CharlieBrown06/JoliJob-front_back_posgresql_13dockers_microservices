package com.example.jolijob.application.adapter.out;

import com.example.jolijob.application.port.out.JobOfferPort;
import com.example.jolijob.joboffer.repository.JobOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JobOfferJpaAdapter implements JobOfferPort {

    private final JobOfferRepository jobOfferRepository;

    @Override
    public Optional<JobOfferSummary> findById(UUID jobOfferId) {
        return jobOfferRepository.findById(jobOfferId)
                .map(offer -> new JobOfferSummary(
                        offer.getId(),
                        offer.isActive(),
                        offer.getCompanyId(),
                        offer.getTitle()));
    }
}
