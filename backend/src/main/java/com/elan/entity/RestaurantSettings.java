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
    @Column(name = "restaurant_timezone", nullable = false, length = 80) public String restaurantTimezone;
    @Column(name = "maximum_advance_booking_days", nullable = false) public int maximumAdvanceBookingDays;
    @Column(name = "minimum_advance_booking_hours", nullable = false) public int minimumAdvanceBookingHours;
    @Column(name = "minimum_party_size", nullable = false) public int minimumPartySize;
    @Column(name = "maximum_online_party_size", nullable = false) public int maximumOnlinePartySize;
    @Column(name = "default_reservation_duration_minutes", nullable = false) public int defaultReservationDurationMinutes;
    @Column(name = "reservation_interval_minutes", nullable = false) public int reservationIntervalMinutes;
    @Column(name = "cancellation_cutoff_hours", nullable = false) public int cancellationCutoffHours;
    @Column(name = "booking_enabled", nullable = false) public boolean bookingEnabled;
    @Column(name = "private_dining_enabled", nullable = false) public boolean privateDiningEnabled;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
}
