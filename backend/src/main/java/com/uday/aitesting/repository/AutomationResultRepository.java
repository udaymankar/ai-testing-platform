package com.uday.aitesting.repository;
import com.uday.aitesting.model.AutomationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface AutomationResultRepository extends JpaRepository<AutomationResult, UUID> {
    List<AutomationResult> findByRun_IdOrderByCreatedAtAsc(UUID runId);
    List<AutomationResult> findByRun_TestingRequest_IdOrderByCreatedAtDesc(UUID requestId);
}
