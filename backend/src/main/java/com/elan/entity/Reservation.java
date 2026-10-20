package com.elan.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id public UUID id;
    @Column(name = "confirmation_code", nullable = false, unique = true, length = 16) public String confirmationCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false) public ReservationGuest guest;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dining_table_id", nullable = false) public DiningTable table;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_id") public DiningExperience experience;
    @Column(name = "party_size", nullable = false) public int partySize;
    @Column(name = "reservation_date", nullable = false) public LocalDate reservationDate;
    @Column(name = "start_time", nullable = false) public LocalTime startTime;
    @Column(name = "end_time", nullable = false) public LocalTime endTime;
    @Column(nullable = false, length = 20) public String status;
    @Column(name = "special_occasion", length = 30) public String specialOccasion;
    @Column(name = "dietary_notes", length = 1000) public String dietaryNotes;
    @Column(name = "special_requests", length = 1000) public String specialRequests;
    @Column(name = "internal_notes", length = 2000) public String internalNotes;
    @Column(name = "management_token_hash", nullable = false, unique = true, length = 64) public String managementTokenHash;
    @Column(name = "management_token_expires_at", nullable = false) public LocalDateTime managementTokenExpiresAt;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
    @Column(name = "cancelled_at") public LocalDateTime cancelledAt;
    @Column(name = "cancellation_reason", length = 500) public String cancellationReason;
    @Version @Column(nullable = false) public long version;
}
