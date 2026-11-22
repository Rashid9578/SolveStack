package com.solvestack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tracker_owners")
public class TrackerOwner {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private TrackerKind kind = TrackerKind.GUEST;

    @Column(name = "normalized_email", length = 254)
    private String normalizedEmail;

    @Column(name = "password_hash")
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TrackerOwner() {}

    public void register(String normalizedEmail, String passwordHash) {
        this.kind = TrackerKind.ACCOUNT;
        this.normalizedEmail = normalizedEmail;
        this.passwordHash = passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public TrackerKind getKind() {
        return kind;
    }

    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
