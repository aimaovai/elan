package com.elan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservation_idempotency_records")
public class ReservationIdempotencyRecord {
    @Id public UUID id;
    @Column(name = "key_hash", nullable = false, unique = true, length = 64) public String keyHash;
    @Column(name = "request_hash", nullable = false, length = 64) public String requestHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false) public Reservation reservation;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false) public LocalDateTime expiresAt;
}
