package com.uday.aitesting.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTestingRequest(
    @NotBlank @Size(max = 2048) String targetUrl,
    @NotBlank @Size(max = 120) String scope,
    @Pattern(regexp = "(?i)chrome|firefox|edge") String browser,
    @AssertTrue(message = "You must confirm that you are authorized to test this website") boolean authorizationConfirmed
) {}
