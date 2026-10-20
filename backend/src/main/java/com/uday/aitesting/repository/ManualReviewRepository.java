package com.uday.aitesting.repository;

import com.uday.aitesting.model.ManualReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ManualReviewRepository extends JpaRepository<ManualReview, UUID> {
    Optional<ManualReview> findByTestingRequest_Id(UUID requestId);
    Optional<ManualReview> findByIdAndTestingRequest_Owner_EmailIgnoreCase(UUID id, String email);
}
