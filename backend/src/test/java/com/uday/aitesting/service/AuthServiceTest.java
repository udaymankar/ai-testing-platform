package com.uday.aitesting.service;

import com.uday.aitesting.dto.RegisterRequest;
import com.uday.aitesting.dto.UserProfile;
import com.uday.aitesting.model.AppUser;
import com.uday.aitesting.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private AppUserRepository repository;
    private PasswordEncoder encoder;
    private AuthService service;
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        repository = mock(AppUserRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        emailVerificationService = mock(EmailVerificationService.class);
        service = new AuthService(repository, encoder, emailVerificationService);
    }

    @Test
    void registrationNormalizesEmailAndStoresOnlyEncodedPassword() {
        when(repository.existsByEmailIgnoreCase("uday@example.com")).thenReturn(false);
        when(repository.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile profile = service.register(new RegisterRequest(" Uday Mankar ", "Uday@Example.com",
                "a-unique-password", "verified-token"));

        assertEquals("Uday Mankar", profile.name());
        assertEquals("uday@example.com", profile.email());
        var captor = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(repository).saveAndFlush(captor.capture());
        assertNotEquals("a-unique-password", captor.getValue().getPasswordHash());
        assertTrue(encoder.matches("a-unique-password", captor.getValue().getPasswordHash()));
    }

    @Test
    void duplicateEmailIsRejected() {
        when(repository.existsByEmailIgnoreCase("uday@example.com")).thenReturn(true);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.register(new RegisterRequest("Uday", "uday@example.com", "a-unique-password", "verified-token")));
        assertEquals(409, error.getStatusCode().value());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void bcryptCompatibleByteLimitIsEnforced() {
        when(repository.existsByEmailIgnoreCase("uday@example.com")).thenReturn(false);
        String password = "é".repeat(40);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.register(new RegisterRequest("Uday", "uday@example.com", password, "verified-token")));
        assertEquals(400, error.getStatusCode().value());
        verify(repository, never()).saveAndFlush(any());
    }
}
