package com.uday.aitesting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "testing_requests")
public class TestingRequest {
    @Id @GeneratedValue
    private UUID id;

    // Nullable for compatibility with any existing pre-registration records. All new requests are owner-assigned.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private AppUser owner;

    @Column(nullable = false, length = 2048)
    private String targetUrl;
    @Column(nullable = false, length = 120)
    private String scope;
    @Column(nullable = false, length = 30)
    private String browser;
    @Column(nullable = false)
    private boolean authorizationConfirmed;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.QUEUED;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(length = 256)
    private String plannedCheckIds;

    protected TestingRequest() {}
    public TestingRequest(String targetUrl, String scope, String browser, boolean authorizationConfirmed, AppUser owner) {
        this.targetUrl = targetUrl;
        this.scope = scope;
        this.browser = browser;
        this.authorizationConfirmed = authorizationConfirmed;
        this.owner = owner;
    }

    @JsonIgnore public AppUser getOwner() { return owner; }
    public UUID getId() { return id; }
    public String getTargetUrl() { return targetUrl; }
    public String getScope() { return scope; }
    public String getBrowser() { return browser; }
    public boolean isAuthorizationConfirmed() { return authorizationConfirmed; }
    public RequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getPlannedCheckIds() { return plannedCheckIds; }
    public void setPlannedCheckIds(String plannedCheckIds) { this.plannedCheckIds = plannedCheckIds; }
    public void setStatus(RequestStatus status) { this.status = status; }
}
