package com.example.jolijob.application.adapter.config;

import com.example.jolijob.application.domain.Actor;
import com.example.jolijob.application.domain.Application;
import com.example.jolijob.application.domain.ApplicationStatus;
import com.example.jolijob.application.port.in.ApplicationUseCase;
import com.example.jolijob.application.port.out.ApplicationRepositoryPort;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

final class TransactionalApplicationUseCase implements ApplicationUseCase {
    private final ApplicationUseCase delegate;
    private final ApplicationRepositoryPort applications;
    private final TransactionTemplate transactions;

    TransactionalApplicationUseCase(ApplicationUseCase delegate, ApplicationRepositoryPort applications,
                                     TransactionTemplate transactions) {
        this.delegate = delegate;
        this.applications = applications;
        this.transactions = transactions;
    }

    @Override
    public Application apply(Actor actor, UUID jobOfferId, String coverLetter, String cvUrl) {
        return transactions.execute(status -> delegate.apply(actor, jobOfferId, coverLetter, cvUrl));
    }

    @Override
    public Application updateStatus(Actor actor, UUID applicationId, ApplicationStatus status) {
        return transactions.execute(transaction -> {
            applications.lockById(applicationId);
            return delegate.updateStatus(actor, applicationId, status);
        });
    }

    @Override
    public Application withdraw(Actor actor, UUID applicationId) {
        return transactions.execute(transaction -> {
            applications.lockById(applicationId);
            return delegate.withdraw(actor, applicationId);
        });
    }

    @Override
    public List<Application> getMyCandidateApplications(Actor actor) {
        return delegate.getMyCandidateApplications(actor);
    }

    @Override
    public List<Application> getOfferApplications(Actor actor, UUID jobOfferId) {
        return delegate.getOfferApplications(actor, jobOfferId);
    }
}
