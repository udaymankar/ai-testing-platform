package com.uday.aitesting.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verifications", uniqueConstraints = @UniqueConstraint(name = "uk_email_verifications_email", columnNames = "email"))
public class EmailVerification {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false, unique=true, length=254) private String email;
    @Column(name="otp_hash", nullable=false, length=100) private String otpHash;
    @Column(name="expires_at", nullable=false) private Instant expiresAt;
    @Column(name="last_sent_at", nullable=false) private Instant lastSentAt;
    @Column(name="verified_at") private Instant verifiedAt;
    @Column(name="verification_token_hash", length=100) private String verificationTokenHash;
    @Column(nullable=false) private int attempts = 0;

    protected EmailVerification() {}
    public EmailVerification(String email, String otpHash, Instant now) {
        this.email=email; this.otpHash=otpHash; this.lastSentAt=now; this.expiresAt=now.plusSeconds(600);
    }
    public String getEmail(){return email;}
    public String getOtpHash(){return otpHash;}
    public Instant getExpiresAt(){return expiresAt;}
    public Instant getLastSentAt(){return lastSentAt;}
    public Instant getVerifiedAt(){return verifiedAt;}
    public int getAttempts(){return attempts;}
    public void replaceCode(String hash, Instant now){otpHash=hash; lastSentAt=now; expiresAt=now.plusSeconds(600); verifiedAt=null; verificationTokenHash=null; attempts=0;}
    public void recordFailedAttempt(){attempts++;}
    public void markVerified(Instant now, String tokenHash){verifiedAt=now; verificationTokenHash=tokenHash;}
    public String getVerificationTokenHash(){return verificationTokenHash;}
    public boolean isExpired(Instant now){return !expiresAt.isAfter(now);}
    public boolean isVerifiedAndFresh(Instant now){return verifiedAt!=null && verifiedAt.isAfter(now.minusSeconds(600)) && !isExpired(now);}
}