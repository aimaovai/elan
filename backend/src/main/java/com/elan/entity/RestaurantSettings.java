package com.elan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "restaurant_settings")
public class RestaurantSettings {
    @Id public UUID id;
    @Column(name = "restaurant_name", nullable = false, length = 80) public String restaurantName;
    @Column(nullable = false, length = 240) public String tagline;
    @Column(nullable = false, columnDefinition = "text") public String description;
    @Column(nullable = false, length = 100) public String city;
    @Column(name = "contact_email", nullable = false, length = 160) public String contactEmail;
    @Column(name = "opening_hours", nullable = false, columnDefinition = "text") public String openingHours;
    @Column(name = "reservation_policy", nullable = false, columnDefinition = "text") public String reservationPolicy;
    @Column(name = "dress_code", nullable = false, columnDefinition = "text") public String dressCode;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
}
