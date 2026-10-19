package com.uday.aitesting.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "testing_requests")
public class TestingRequest {
    @Id @GeneratedValue
    private UUID id;
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

    protected TestingRequest() {}
    public TestingRequest(String targetUrl, String scope, String browser, boolean authorizationConfirmed) {
        this.targetUrl = targetUrl; this.scope = scope; this.browser = browser;
        this.authorizationConfirmed = authorizationConfirmed;
    }
    public UUID getId() { return id; }
    public String getTargetUrl() { return targetUrl; }
    public String getScope() { return scope; }
    public String getBrowser() { return browser; }
    public boolean isAuthorizationConfirmed() { return authorizationConfirmed; }
    public RequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(RequestStatus status) { this.status = status; }
}
