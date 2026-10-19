package com.uday.aitesting.service;

import com.uday.aitesting.dto.CreateTestingRequest;
import com.uday.aitesting.model.*;
import com.uday.aitesting.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.UUID;

@Service
public class TestingRequestService {
    private final TestingRequestRepository requestRepository;
    private final AutomationRunRepository runRepository;
    private final ManualReviewRepository manualReviewRepository;
    private final UrlSafetyService urlSafetyService;
    private final BrowserAutomationService browserAutomationService;

    public TestingRequestService(TestingRequestRepository requestRepository, AutomationRunRepository runRepository,
                                 ManualReviewRepository manualReviewRepository, UrlSafetyService urlSafetyService,
                                 BrowserAutomationService browserAutomationService) {
        this.requestRepository = requestRepository; this.runRepository = runRepository;
        this.manualReviewRepository = manualReviewRepository; this.urlSafetyService = urlSafetyService;
        this.browserAutomationService = browserAutomationService;
    }

    @Transactional
    public TestingRequest create(CreateTestingRequest dto) {
        URI uri = urlSafetyService.validateAndNormalize(dto.targetUrl());
        TestingRequest request = requestRepository.save(new TestingRequest(uri.toString(), dto.scope(),
                dto.browser() == null ? "Chrome" : dto.browser(), dto.authorizationConfirmed()));
        manualReviewRepository.save(new ManualReview(request));
        return request;
    }

    @Transactional
    public AutomationRun start(UUID requestId) {
        TestingRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Testing request not found."));
        AutomationRun run = runRepository.save(new AutomationRun(request));
        browserAutomationService.executeAsync(run.getId());
        return run;
    }
}
