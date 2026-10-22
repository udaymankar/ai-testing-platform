package com.uday.aitesting.repository;

import com.uday.aitesting.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    Optional<EmailVerification> findByEmailIgnoreCase(String email);
    void deleteByEmailIgnoreCase(String email);
}