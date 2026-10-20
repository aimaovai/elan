package com.elan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dining_tables")
public class DiningTable {
    @Id public UUID id;
    @Column(name = "table_number", nullable = false, unique = true, length = 30) public String tableNumber;
    @Column(name = "display_name", nullable = false, length = 80) public String displayName;
    @Column(name = "minimum_capacity", nullable = false) public int minimumCapacity;
    @Column(name = "maximum_capacity", nullable = false) public int maximumCapacity;
    @Column(nullable = false, length = 40) public String location;
    @Column(name = "is_active", nullable = false) public boolean active;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
}
