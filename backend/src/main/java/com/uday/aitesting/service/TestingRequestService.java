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
    private final AutomationRunRepository runRepository;
    private final ManualReviewRepository manualReviewRepository;
    private final UrlSafetyService urlSafetyService;
    private final BrowserAutomationService browserAutomationService;
    private final TestPlanService testPlanService;

    public TestingRequestService(TestingRequestRepository requestRepository, AutomationRunRepository runRepository,
                                 ManualReviewRepository manualReviewRepository, UrlSafetyService urlSafetyService,
                                 BrowserAutomationService browserAutomationService, TestPlanService testPlanService) {
        this.requestRepository = requestRepository; this.runRepository = runRepository;
        this.manualReviewRepository = manualReviewRepository; this.urlSafetyService = urlSafetyService;
        this.browserAutomationService = browserAutomationService; this.testPlanService = testPlanService;
    }

    @Transactional
    public TestingRequest create(CreateTestingRequest dto) {
        URI uri = urlSafetyService.validateAndNormalize(dto.targetUrl());
        TestingRequest newRequest = new TestingRequest(uri.toString(), dto.scope(),
                dto.browser() == null ? "Chrome" : dto.browser(), dto.authorizationConfirmed());
        newRequest.setPlannedCheckIds(testPlanService.createPlan(dto.scope(), uri.toString()).stream()
                .map(TestPlanService.PlannedCheck::id).collect(java.util.stream.Collectors.joining(",")));
        TestingRequest request = requestRepository.save(newRequest);
        manualReviewRepository.save(new ManualReview(request));
        return request;
    }

    @Transactional
    public AutomationRun start(UUID requestId) {
        TestingRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Testing request not found."));
        AutomationRun run = runRepository.save(new AutomationRun(request));
        UUID runId = run.getId();
        // Dispatch only after commit so a fast worker cannot race the uncommitted run row.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                browserAutomationService.executeAsync(runId);
            }
        });
        return run;
    }
}
