package com.uday.aitesting.dto;
import com.uday.aitesting.model.ManualStatus;
import jakarta.validation.constraints.Size;
public record UpdateManualReview(ManualStatus status, @Size(max = 200) String assignedTester,
                                 @Size(max = 2000) String reviewerNotes) {}
