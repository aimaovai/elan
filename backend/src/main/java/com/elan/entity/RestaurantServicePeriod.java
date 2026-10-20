package com.elan.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "restaurant_service_periods")
public class RestaurantServicePeriod {
    @Id public UUID id;
    @Column(nullable = false, length = 80) public String name;
    @Column(name = "day_of_week", nullable = false) public int dayOfWeek;
    @Column(name = "start_time", nullable = false) public LocalTime startTime;
    @Column(name = "end_time", nullable = false) public LocalTime endTime;
    @Column(name = "slot_interval_minutes", nullable = false) public int slotIntervalMinutes;
    @Column(name = "dining_duration_minutes", nullable = false) public int diningDurationMinutes;
    @Column(name = "is_active", nullable = false) public boolean active;
}
