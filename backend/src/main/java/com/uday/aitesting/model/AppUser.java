package com.uday.aitesting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(name = "uk_app_users_email", columnNames = "email"))
public class AppUser {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {}

    public AppUser(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    @JsonIgnore public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}
