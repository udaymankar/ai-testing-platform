package com.uday.aitesting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "automation_runs")
public class AutomationRun {
    @Id @GeneratedValue
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private TestingRequest testingRequest;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private RunStatus status = RunStatus.QUEUED;
    private Instant startedAt;
    private Instant completedAt;
    @Column(length = 2000)
    private String errorMessage;

    protected AutomationRun() {}
    public AutomationRun(TestingRequest request) { this.testingRequest = request; }
    public UUID getId() { return id; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public TestingRequest getTestingRequest() { return testingRequest; }
    public RunStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void start() { status = RunStatus.RUNNING; startedAt = Instant.now(); }
    public void complete() { status = RunStatus.COMPLETED; completedAt = Instant.now(); }
    public void fail(String message) { status = RunStatus.FAILED; errorMessage = message; completedAt = Instant.now(); }
}
