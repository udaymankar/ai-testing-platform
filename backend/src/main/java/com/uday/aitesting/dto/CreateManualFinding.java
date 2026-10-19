package com.uday.aitesting.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreateManualFinding(@NotBlank @Size(max=180) String title,
                                  @NotBlank @Size(max=4000) String description,
                                  @NotBlank String severity,
                                  @Size(max=2048) String evidenceUrl) {}
