package com.uday.aitesting.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UrlSafetyServiceTest {
    private final UrlSafetyService service = new UrlSafetyService();

    @Test void acceptsPublicHttpsUrl() {
        // DNS is consulted by this validator; use a stable public hostname only in integration environments.
        assertDoesNotThrow(() -> service.validateAndNormalize("https://example.com"));
    }

    @Test void rejectsLocalhost() {
        assertThrows(IllegalArgumentException.class, () -> service.validateAndNormalize("http://localhost:8080"));
    }

    @Test void rejectsUnsupportedScheme() {
        assertThrows(IllegalArgumentException.class, () -> service.validateAndNormalize("file:///etc/passwd"));
    }
}
