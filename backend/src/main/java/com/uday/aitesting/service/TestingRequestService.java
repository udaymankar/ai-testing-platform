package com.uday.aitesting.service;

import com.uday.aitesting.dto.CreateTestingRequest;
import com.uday.aitesting.model.*;
import com.uday.aitesting.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.util.UUID;

@Service
public class TestingRequestService {
    private final TestingRequestRepository requestRepository;
    private final AppUserRepository userRepository;
    private final AutomationRunRepository runRepository;
    private final ManualReviewRepository manualReviewRepository;
    private final UrlSafetyService urlSafetyService;
    private final BrowserAutomationService browserAutomationService;
    private final TestPlanService testPlanService;

    public TestingRequestService(TestingRequestRepository requestRepository, AppUserRepository userRepository,
                                 AutomationRunRepository runRepository, ManualReviewRepository manualReviewRepository,
                                 UrlSafetyService urlSafetyService, BrowserAutomationService browserAutomationService,
                                 TestPlanService testPlanService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.runRepository = runRepository;
        this.manualReviewRepository = manualReviewRepository;
        this.urlSafetyService = urlSafetyService;
        this.browserAutomationService = browserAutomationService;
        this.testPlanService = testPlanService;
    }

    @Transactional
    public TestingRequest create(CreateTestingRequest dto, String authenticatedEmail) {
        AppUser owner = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated account no longer exists."));
        URI uri = urlSafetyService.validateAndNormalize(dto.targetUrl());
        TestingRequest newRequest = new TestingRequest(uri.toString(), dto.scope(),
                dto.browser() == null ? "Chrome" : dto.browser(), dto.authorizationConfirmed(), owner);
        newRequest.setPlannedCheckIds(testPlanService.createPlan(dto.scope(), uri.toString()).stream()
                .map(TestPlanService.PlannedCheck::id).collect(java.util.stream.Collectors.joining(",")));
        TestingRequest request = requestRepository.save(newRequest);
        manualReviewRepository.save(new ManualReview(request));
        return request;
    }

    @Transactional
    public AutomationRun start(UUID requestId, String authenticatedEmail) {
        TestingRequest request = requestRepository.findByIdAndOwner_EmailIgnoreCase(requestId, authenticatedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Testing request not found."));
        AutomationRun run = runRepository.save(new AutomationRun(request));
        UUID runId = run.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                browserAutomationService.executeAsync(runId);
            }
        });
        return run;
    }
}
