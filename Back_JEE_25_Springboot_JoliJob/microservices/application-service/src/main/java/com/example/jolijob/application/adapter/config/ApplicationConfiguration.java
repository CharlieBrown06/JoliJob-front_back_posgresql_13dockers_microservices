package com.example.jolijob.application.adapter.config;

import com.example.jolijob.application.applicationservice.ApplicationService;
import com.example.jolijob.application.port.in.ApplicationUseCase;
import com.example.jolijob.application.port.out.ApplicationEventPort;
import com.example.jolijob.application.port.out.ApplicationRepositoryPort;
import com.example.jolijob.application.port.out.CandidateProfilePort;
import com.example.jolijob.application.port.out.CompanyProfilePort;
import com.example.jolijob.application.port.out.JobOfferPort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ApplicationConfiguration {

    @Bean
    ApplicationUseCase applicationUseCase(
            ApplicationRepositoryPort applications,
            CandidateProfilePort candidates,
            CompanyProfilePort companies,
            JobOfferPort jobOffers,
            ApplicationEventPort events,
            PlatformTransactionManager transactionManager) {
        ApplicationUseCase useCase = new ApplicationService(applications, candidates, companies, jobOffers, events);
        return new TransactionalApplicationUseCase(
                useCase, applications, new TransactionTemplate(transactionManager));
    }
}
