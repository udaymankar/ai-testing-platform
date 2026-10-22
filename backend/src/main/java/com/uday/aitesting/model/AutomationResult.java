package com.uday.aitesting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "automation_results")
public class AutomationResult {
    @Id @GeneratedValue
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AutomationRun run;
    @Column(nullable = false, length = 180)
    private String testName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TestOutcome outcome;
    @Column(length = 3000)
    private String details;
    @Column(length = 2048)
    private String screenshotPath;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected AutomationResult() {}
    public AutomationResult(AutomationRun run, String testName, TestOutcome outcome, String details, String screenshotPath) {
        this.run = run; this.testName = testName; this.outcome = outcome;
        this.details = details; this.screenshotPath = screenshotPath;
    }
    public UUID getId() { return id; }
    @JsonIgnore
    public AutomationRun getRun() { return run; }
    @Transient
    public UUID getRunId() { return run.getId(); }
    public String getTestName() { return testName; }
    public TestOutcome getOutcome() { return outcome; }
    public String getDetails() { return details; }
    public String getScreenshotPath() { return screenshotPath; }
    public Instant getCreatedAt() { return createdAt; }
}
