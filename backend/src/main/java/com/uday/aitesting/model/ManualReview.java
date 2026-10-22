package com.uday.aitesting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "manual_reviews")
public class ManualReview {
    @Id @GeneratedValue
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "testing_request_id", unique = true)
    private TestingRequest testingRequest;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ManualStatus status = ManualStatus.PENDING;
    @Column(length = 200)
    private String assignedTester;
    @Column(length = 2000)
    private String reviewerNotes;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected ManualReview() {}
    public ManualReview(TestingRequest request) { this.testingRequest = request; }
    public UUID getId() { return id; }
    @JsonIgnore
    public TestingRequest getTestingRequest() { return testingRequest; }
    @Transient
    public UUID getRequestId() { return testingRequest.getId(); }
    public ManualStatus getStatus() { return status; }
    public String getAssignedTester() { return assignedTester; }
    public String getReviewerNotes() { return reviewerNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void update(ManualStatus status, String assignedTester, String notes) {
        this.status = status;
        this.assignedTester = assignedTester;
        this.reviewerNotes = notes;
        this.updatedAt = Instant.now();
    }
}
