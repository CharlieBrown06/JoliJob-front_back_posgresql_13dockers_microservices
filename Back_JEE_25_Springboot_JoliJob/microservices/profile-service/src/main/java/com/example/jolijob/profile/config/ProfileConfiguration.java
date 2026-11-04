package com.example.jolijob.profile.config;

import com.example.jolijob.profile.application.CandidateProfiles;
import com.example.jolijob.profile.application.CompanyProfiles;
import com.example.jolijob.profile.application.port.in.CandidateProfileUseCase;
import com.example.jolijob.profile.application.port.in.CompanyProfileUseCase;
import com.example.jolijob.profile.application.port.out.CandidateProfileRepository;
import com.example.jolijob.profile.application.port.out.CompanyProfileRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ProfileConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CandidateProfileUseCase candidateProfiles(CandidateProfileRepository repository, Clock clock) {
        return new CandidateProfiles(repository, clock);
    }

    @Bean
    CompanyProfileUseCase companyProfiles(CompanyProfileRepository repository, Clock clock) {
        return new CompanyProfiles(repository, clock);
    }
}
