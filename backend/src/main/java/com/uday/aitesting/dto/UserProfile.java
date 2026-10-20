package com.uday.aitesting.dto;

import com.uday.aitesting.model.AppUser;
import java.time.Instant;
import java.util.UUID;

public record UserProfile(UUID id, String name, String email, Instant createdAt) {
    public static UserProfile from(AppUser user) {
        return new UserProfile(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
