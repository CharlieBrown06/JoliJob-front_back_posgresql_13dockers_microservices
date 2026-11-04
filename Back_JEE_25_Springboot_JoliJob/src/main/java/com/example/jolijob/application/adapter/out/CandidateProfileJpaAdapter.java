package com.example.jolijob.application.adapter.out;

import com.example.jolijob.application.port.out.CandidateProfilePort;
import com.example.jolijob.candidate.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CandidateProfileJpaAdapter implements CandidateProfilePort {

    private final CandidateRepository candidateRepository;

    @Override
    public Optional<CandidateProfile> findByUserId(UUID userId) {
        return candidateRepository.findByUserId(userId)
                .map(candidate -> new CandidateProfile(candidate.getId(), candidate.getCvUrl()));
    }
}
