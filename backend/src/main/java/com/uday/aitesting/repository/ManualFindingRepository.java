package com.uday.aitesting.repository;
import com.uday.aitesting.model.ManualFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface ManualFindingRepository extends JpaRepository<ManualFinding, UUID> {
    List<ManualFinding> findByManualReview_IdOrderByCreatedAtDesc(UUID reviewId);
}
