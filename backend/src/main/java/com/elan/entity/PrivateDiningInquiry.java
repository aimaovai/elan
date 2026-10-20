package com.elan.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "private_dining_inquiries")
public class PrivateDiningInquiry {
    @Id public UUID id;
    @Column(name = "reference_number", nullable = false, unique = true, length = 16) public String referenceNumber;
    @Column(name = "first_name", nullable = false, length = 80) public String firstName;
    @Column(name = "last_name", nullable = false, length = 80) public String lastName;
    @Column(nullable = false, length = 254) public String email;
    @Column(name = "phone_number", nullable = false, length = 32) public String phoneNumber;
    @Column(name = "event_type", nullable = false, length = 30) public String eventType;
    @Column(name = "preferred_date", nullable = false) public LocalDate preferredDate;
    @Column(name = "alternate_date") public LocalDate alternateDate;
    @Column(name = "guest_count", nullable = false) public int guestCount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "private_dining_space_id") public PrivateDiningSpace space;
    @Column(name = "budget_per_guest", precision = 8, scale = 2) public BigDecimal budgetPerGuest;
    @Column(name = "dietary_requirements", length = 1000) public String dietaryRequirements;
    @Column(name = "beverage_preferences", length = 1000) public String beveragePreferences;
    @Column(name = "event_description", nullable = false, length = 2000) public String eventDescription;
    @Column(name = "special_requests", length = 1000) public String specialRequests;
    @Column(name = "preferred_contact_method", nullable = false, length = 20) public String preferredContactMethod;
    @Column(nullable = false, length = 20) public String status;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
}
