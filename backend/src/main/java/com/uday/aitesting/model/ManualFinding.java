package com.uday.aitesting.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "manual_findings")
public class ManualFinding {
    @Id @GeneratedValue
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ManualReview manualReview;
    @Column(nullable = false, length = 180)
    private String title;
    @Column(nullable = false, length = 4000)
    private String description;
    @Column(nullable = false, length = 20)
    private String severity;
    @Column(length = 2048)
    private String evidenceUrl;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ManualFinding() {}
    public ManualFinding(ManualReview review, String title, String description, String severity, String evidenceUrl) {
        this.manualReview = review; this.title = title; this.description = description;
        this.severity = severity; this.evidenceUrl = evidenceUrl;
    }
    public UUID getId() { return id; }
    public UUID getManualReviewId() { return manualReview.getId(); }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSeverity() { return severity; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
