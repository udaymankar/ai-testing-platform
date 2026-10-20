package com.uday.aitesting.controller;

import com.uday.aitesting.dto.RegisterRequest;
import com.uday.aitesting.dto.UserProfile;
import com.uday.aitesting.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public UserProfile register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // Spring Security validates the HTTP Basic credentials before the controller is called.
    @PostMapping("/login")
    public UserProfile login(Authentication authentication) {
        return authService.profile(authentication.getName());
    }
}
