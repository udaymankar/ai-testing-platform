package com.uday.aitesting.repository;

import com.uday.aitesting.model.AutomationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutomationRunRepository extends JpaRepository<AutomationRun, UUID> {
    List<AutomationRun> findByTestingRequest_IdOrderByStartedAtDesc(UUID requestId);

    @Query("select r from AutomationRun r join fetch r.testingRequest where r.id = :id")
    Optional<AutomationRun> findWithTestingRequestById(@Param("id") UUID id);
}
