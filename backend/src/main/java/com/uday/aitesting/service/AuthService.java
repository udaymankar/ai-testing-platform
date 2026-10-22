package com.uday.aitesting.service;

import com.uday.aitesting.dto.RegisterRequest;
import com.uday.aitesting.dto.UserProfile;
import com.uday.aitesting.model.AppUser;
import com.uday.aitesting.repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public UserProfile register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String name = request.name().trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be no more than 72 UTF-8 bytes for BCrypt compatibility.");
        }
        emailVerificationService.requireVerifiedAndConsume(email);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        AppUser user = new AppUser(name, email, passwordEncoder.encode(request.password()));
        try {
            return UserProfile.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists.");
        }
    }

    @Transactional(readOnly = true)
    public UserProfile profile(String authenticatedEmail) {
        AppUser user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        return UserProfile.from(user);
    }
}
