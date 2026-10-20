package com.elan.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "private_dining_spaces")
public class PrivateDiningSpace {
    @Id public UUID id;
    @Column(nullable = false, length = 120) public String name;
    @Column(nullable = false, unique = true, length = 120) public String slug;
    @Column(nullable = false, columnDefinition = "text") public String description;
    @Column(nullable = false) public int capacity;
    @Column(name = "image_url", nullable = false, columnDefinition = "text") public String imageUrl;
    @Column(name = "display_order", nullable = false) public int displayOrder;
    @Column(name = "is_active", nullable = false) public boolean active;
}
