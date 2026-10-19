package com.uday.aitesting.controller;

import com.uday.aitesting.dto.*;
import com.uday.aitesting.model.*;
import com.uday.aitesting.repository.*;
import com.uday.aitesting.service.TestingRequestService;
import com.uday.aitesting.service.TestPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
        this.requestRepository = requestRepository; this.runRepository = runRepository;
        this.resultRepository = resultRepository; this.reviewRepository = reviewRepository;
        this.findingRepository = findingRepository; this.requestService = requestService;
        this.testPlanService = testPlanService;
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP", "service", "ai-testing-platform"); }

    @PostMapping("/testing-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody CreateTestingRequest dto) {
        TestingRequest request = requestService.create(dto);
        ManualReview review = reviewRepository.findByTestingRequest_Id(request.getId()).orElseThrow();
        return Map.of("request", request, "manualReview", review);
    }

    @GetMapping("/testing-requests")
    public List<TestingRequest> list() {
        return requestRepository.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @GetMapping("/testing-requests/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        TestingRequest request = requestRepository.findById(id).orElseThrow(() -> notFound("Request not found"));
        ManualReview review = reviewRepository.findByTestingRequest_Id(id).orElseThrow();
        return Map.of("request", request, "manualReview", review,
                "runs", runRepository.findByTestingRequest_IdOrderByStartedAtDesc(id));
    }

    @GetMapping("/testing-requests/{id}/plan")
    public List<TestPlanService.PlannedCheck> plan(@PathVariable UUID id) {
        TestingRequest request = requestRepository.findById(id)
                .orElseThrow(() -> notFound("Request not found"));
        return testPlanService.restorePlan(request.getPlannedCheckIds());
    }

    @PostMapping("/testing-requests/{id}/automation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AutomationRun start(@PathVariable UUID id) {
        try { return requestService.start(id); }
        catch (IllegalArgumentException ex) { throw notFound(ex.getMessage()); }
    }

    @GetMapping("/testing-requests/{id}/results")
    public List<AutomationResult> results(@PathVariable UUID id) {
        if (!requestRepository.existsById(id)) throw notFound("Request not found");
        return resultRepository.findByRun_TestingRequest_IdOrderByCreatedAtDesc(id);
    }

    @PatchMapping("/manual-reviews/{id}")
    public ManualReview updateReview(@PathVariable UUID id, @Valid @RequestBody UpdateManualReview dto) {
        ManualReview review = reviewRepository.findById(id).orElseThrow(() -> notFound("Manual review not found"));
        review.update(dto.status() == null ? review.getStatus() : dto.status(),
                dto.assignedTester(), dto.reviewerNotes());
        return reviewRepository.save(review);
    }

    @PostMapping("/manual-reviews/{id}/findings")
    @ResponseStatus(HttpStatus.CREATED)
    public ManualFinding addFinding(@PathVariable UUID id, @Valid @RequestBody CreateManualFinding dto) {
        ManualReview review = reviewRepository.findById(id).orElseThrow(() -> notFound("Manual review not found"));
        Set<String> allowed = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
        String severity = dto.severity().toUpperCase(Locale.ROOT);
        if (!allowed.contains(severity)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid severity");
        ManualFinding finding = findingRepository.save(new ManualFinding(review, dto.title(), dto.description(),
                severity, dto.evidenceUrl()));
        return finding;
    }

    @GetMapping("/manual-reviews/{id}/findings")
    public List<ManualFinding> findings(@PathVariable UUID id) {
        if (!reviewRepository.existsById(id)) throw notFound("Manual review not found");
        return findingRepository.findByManualReview_IdOrderByCreatedAtDesc(id);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
