package com.elan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "administrators")
public class Administrator {
    @Id public UUID id;
    @Column(nullable = false, unique = true, length = 254) public String email;
    @Column(name = "password_hash", nullable = false, length = 100) public String passwordHash;
    @Column(name = "display_name", nullable = false, length = 120) public String displayName;
    @Column(nullable = false, length = 20) public String role;
    @Column(name = "is_active", nullable = false) public boolean active;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
    @Column(name = "last_login_at") public LocalDateTime lastLoginAt;
}
