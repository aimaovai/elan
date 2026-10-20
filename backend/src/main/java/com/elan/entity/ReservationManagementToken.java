package com.elan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservation_management_tokens")
public class ReservationManagementToken {
    @Id public UUID id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "reservation_id", nullable = false) public Reservation reservation;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) public String tokenHash;
    @Column(name = "expires_at", nullable = false) public LocalDateTime expiresAt;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
}
