package com.elan.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "restaurant_blocked_dates")
public class RestaurantBlockedDate {
    @Id public UUID id;
    @Column(name = "blocked_date", nullable = false) public LocalDate blockedDate;
    @Column(nullable = false, length = 160) public String reason;
    @Column(name = "is_full_day", nullable = false) public boolean fullDay;
    @Column(name = "start_time") public LocalTime startTime;
    @Column(name = "end_time") public LocalTime endTime;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
}
