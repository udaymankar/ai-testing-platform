package com.uday.aitesting.repository;

import com.uday.aitesting.model.TestingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestingRequestRepository extends JpaRepository<TestingRequest, UUID> {
    List<TestingRequest> findAllByOwner_EmailIgnoreCaseOrderByCreatedAtDesc(String email);
    Optional<TestingRequest> findByIdAndOwner_EmailIgnoreCase(UUID id, String email);
}
