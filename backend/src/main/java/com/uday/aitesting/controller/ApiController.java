package com.uday.aitesting.controller;

import com.uday.aitesting.dto.*;
import com.uday.aitesting.model.*;
import com.uday.aitesting.repository.*;
import com.uday.aitesting.service.TestingRequestService;
import com.uday.aitesting.service.TestPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final TestingRequestRepository requestRepository;
    private final AutomationRunRepository runRepository;
    private final AutomationResultRepository resultRepository;
    private final ManualReviewRepository reviewRepository;
    private final ManualFindingRepository findingRepository;
    private final TestingRequestService requestService;
    private final TestPlanService testPlanService;

    public ApiController(TestingRequestRepository requestRepository, AutomationRunRepository runRepository,
                         AutomationResultRepository resultRepository, ManualReviewRepository reviewRepository,
                         ManualFindingRepository findingRepository, TestingRequestService requestService,
                         TestPlanService testPlanService) {
        this.requestRepository = requestRepository;
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.reviewRepository = reviewRepository;
        this.findingRepository = findingRepository;
        this.requestService = requestService;
        this.testPlanService = testPlanService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "ai-testing-platform");
    }

    @PostMapping("/testing-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody CreateTestingRequest dto, Authentication authentication) {
        TestingRequest request = requestService.create(dto, authentication.getName());
        ManualReview review = reviewRepository.findByTestingRequest_Id(request.getId()).orElseThrow();
        return Map.of("request", request, "manualReview", review);
    }

    @GetMapping("/testing-requests")
    public List<TestingRequest> list(Authentication authentication) {
        return requestRepository.findAllByOwner_EmailIgnoreCaseOrderByCreatedAtDesc(authentication.getName());
    }

    @GetMapping("/testing-requests/{id}")
    public Map<String, Object> get(@PathVariable UUID id, Authentication authentication) {
        TestingRequest request = ownedRequest(id, authentication.getName());
        ManualReview review = reviewRepository.findByTestingRequest_Id(request.getId()).orElseThrow();
        return Map.of("request", request, "manualReview", review,
                "runs", runRepository.findByTestingRequest_IdOrderByStartedAtDesc(id));
    }

    @GetMapping("/testing-requests/{id}/plan")
    public List<TestPlanService.PlannedCheck> plan(@PathVariable UUID id, Authentication authentication) {
        TestingRequest request = ownedRequest(id, authentication.getName());
        return testPlanService.restorePlan(request.getPlannedCheckIds());
    }

    @PostMapping("/testing-requests/{id}/automation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AutomationRun start(@PathVariable UUID id, Authentication authentication) {
        try {
            return requestService.start(id, authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw notFound(ex.getMessage());
        }
    }

    @GetMapping("/testing-requests/{id}/results")
    public List<AutomationResult> results(@PathVariable UUID id, Authentication authentication) {
        ownedRequest(id, authentication.getName());
        return resultRepository.findByRun_TestingRequest_IdOrderByCreatedAtDesc(id);
    }

    @PatchMapping("/manual-reviews/{id}")
    public ManualReview updateReview(@PathVariable UUID id, @Valid @RequestBody UpdateManualReview dto,
                                     Authentication authentication) {
        ManualReview review = ownedReview(id, authentication.getName());
        review.update(dto.status() == null ? review.getStatus() : dto.status(),
                dto.assignedTester(), dto.reviewerNotes());
        return reviewRepository.save(review);
    }

    @PostMapping("/manual-reviews/{id}/findings")
    @ResponseStatus(HttpStatus.CREATED)
    public ManualFinding addFinding(@PathVariable UUID id, @Valid @RequestBody CreateManualFinding dto,
                                    Authentication authentication) {
        ManualReview review = ownedReview(id, authentication.getName());
        Set<String> allowed = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
        String severity = dto.severity().toUpperCase(Locale.ROOT);
        if (!allowed.contains(severity)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid severity");
        }
        return findingRepository.save(new ManualFinding(review, dto.title(), dto.description(),
                severity, dto.evidenceUrl()));
    }

    @GetMapping("/manual-reviews/{id}/findings")
    public List<ManualFinding> findings(@PathVariable UUID id, Authentication authentication) {
        ownedReview(id, authentication.getName());
        return findingRepository.findByManualReview_IdOrderByCreatedAtDesc(id);
    }

    private TestingRequest ownedRequest(UUID id, String email) {
        return requestRepository.findByIdAndOwner_EmailIgnoreCase(id, email)
                .orElseThrow(() -> notFound("Testing request not found."));
    }

    private ManualReview ownedReview(UUID id, String email) {
        return reviewRepository.findByIdAndTestingRequest_Owner_EmailIgnoreCase(id, email)
                .orElseThrow(() -> notFound("Manual review not found."));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
