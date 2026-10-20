package com.elan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservation_guests")
public class ReservationGuest {
    @Id public UUID id;
    @Column(name = "first_name", nullable = false, length = 80) public String firstName;
    @Column(name = "last_name", nullable = false, length = 80) public String lastName;
    @Column(nullable = false, length = 254) public String email;
    @Column(name = "phone_number", nullable = false, length = 32) public String phoneNumber;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
}
