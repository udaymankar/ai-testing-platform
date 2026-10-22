package com.uday.aitesting.controller;

import com.uday.aitesting.dto.EmailOtpRequest;
import com.uday.aitesting.dto.VerifyEmailOtpRequest;
import com.uday.aitesting.dto.RegisterRequest;
import com.uday.aitesting.dto.UserProfile;
import com.uday.aitesting.service.AuthService;
import com.uday.aitesting.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/send-otp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendOtp(@Valid @RequestBody EmailOtpRequest request) {
        emailVerificationService.sendCode(request.email());
    }

    @PostMapping("/verify-otp")
    public java.util.Map<String, String> verifyOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return java.util.Map.of("verificationToken", emailVerificationService.verify(request.email(), request.otp()));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfile register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public UserProfile login(Authentication authentication) {
        return authService.profile(authentication.getName());
    }
}