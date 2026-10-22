package com.uday.aitesting.service;

import com.uday.aitesting.model.EmailVerification;
import com.uday.aitesting.repository.AppUserRepository;
import com.uday.aitesting.repository.EmailVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

@Service
public class EmailVerificationService {
    private final EmailVerificationRepository verificationRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${app.mail.from:}") private String fromAddress;

    public EmailVerificationService(EmailVerificationRepository verificationRepository, AppUserRepository userRepository,
                                    PasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.verificationRepository=verificationRepository; this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder; this.mailSender=mailSender;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void sendCode(String rawEmail) {
        String email=normalize(rawEmail);
        if (userRepository.existsByEmailIgnoreCase(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        Instant now=Instant.now();
        EmailVerification current=verificationRepository.findByEmailIgnoreCase(email).orElse(null);
        if (current!=null && current.getLastSentAt().isAfter(now.minusSeconds(60)))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait 60 seconds before requesting another code.");
        if (fromAddress==null || fromAddress.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email verification is not configured yet. Please try again later.");
        String otp=String.format(Locale.ROOT,"%06d",secureRandom.nextInt(1_000_000));
        if(current==null) current=new EmailVerification(email,passwordEncoder.encode(otp),now);
        else current.replaceCode(passwordEncoder.encode(otp),now);
        verificationRepository.saveAndFlush(current);
        try {
            SimpleMailMessage message=new SimpleMailMessage();
            message.setFrom(fromAddress); message.setTo(email); message.setSubject("Your TestPilot AI verification code");
            message.setText("Your TestPilot AI verification code is: "+otp+"\n\nIt expires in 10 minutes. Do not share this code with anyone. If you did not request it, you can ignore this email.");
            mailSender.send(message);
        } catch (MailException ex) {
            verificationRepository.deleteByEmailIgnoreCase(email);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "We could not send the verification email. Please try again later.");
        }
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public String verify(String rawEmail, String otp) {
        String email=normalize(rawEmail);
        EmailVerification record=verificationRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Request a verification code first."));
        Instant now=Instant.now();
        if(record.isExpired(now)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This code has expired. Request a new one.");
        if(record.getAttempts()>=5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many incorrect attempts. Request a new code.");
        if(!passwordEncoder.matches(otp,record.getOtpHash())) {
            record.recordFailedAttempt(); verificationRepository.save(record);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Incorrect verification code.");
        }
        String verificationToken = java.util.UUID.randomUUID().toString();
        record.markVerified(now, passwordEncoder.encode(verificationToken)); verificationRepository.save(record);
        return verificationToken;
    }

    @Transactional
    public void requireVerifiedAndConsume(String rawEmail, String verificationToken) {
        String email=normalize(rawEmail);
        EmailVerification record=verificationRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Verify your email before creating an account."));
        if(!record.isVerifiedAndFresh(Instant.now()) || verificationToken == null || record.getVerificationTokenHash() == null || !passwordEncoder.matches(verificationToken, record.getVerificationTokenHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Email verification is missing, invalid or expired. Verify your email again.");
        verificationRepository.delete(record);
    }

    private String normalize(String email){return email.trim().toLowerCase(Locale.ROOT);}
}