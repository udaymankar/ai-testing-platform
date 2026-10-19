package com.uday.aitesting.repository;
import com.uday.aitesting.model.TestingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface TestingRequestRepository extends JpaRepository<TestingRequest, UUID> {}
