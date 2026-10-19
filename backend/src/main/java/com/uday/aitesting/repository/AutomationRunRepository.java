package com.uday.aitesting.repository;
import com.uday.aitesting.model.AutomationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface AutomationRunRepository extends JpaRepository<AutomationRun, UUID> {
    List<AutomationRun> findByTestingRequest_IdOrderByStartedAtDesc(UUID requestId);
}
