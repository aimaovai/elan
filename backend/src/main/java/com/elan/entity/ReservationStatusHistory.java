package com.elan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservation_status_history")
public class ReservationStatusHistory {
    @Id public UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false) public Reservation reservation;
    @Column(name = "old_status", length = 20) public String oldStatus;
    @Column(name = "new_status", nullable = false, length = 20) public String newStatus;
    @Column(name = "change_source", nullable = false, length = 20) public String changeSource;
    @Column(name = "administrator_id") public UUID administratorId;
    @Column(length = 500) public String reason;
    @Column(name = "changed_at", nullable = false) public LocalDateTime changedAt;
}
