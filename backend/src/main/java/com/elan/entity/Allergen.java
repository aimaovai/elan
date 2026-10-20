package com.elan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "allergens")
public class Allergen {
    @Id public UUID id;
    @Column(nullable = false, unique = true, length = 50) public String name;
}
