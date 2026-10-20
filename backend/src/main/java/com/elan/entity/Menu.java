package com.elan.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "menus")
public class Menu {
    @Id public UUID id;
    @Column(nullable = false, length = 100) public String name;
    @Column(nullable = false, unique = true, length = 100) public String slug;
    @Column(nullable = false, columnDefinition = "text") public String description;
    @Column(name = "menu_type", nullable = false, length = 30) public String menuType;
    @Column(name = "is_published", nullable = false) public boolean published;
    @Column(name = "valid_from") public LocalDate validFrom;
    @Column(name = "valid_until") public LocalDate validUntil;
    @Column(name = "display_order", nullable = false) public int displayOrder;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
    @OneToMany(mappedBy = "menu", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    public List<MenuCategory> categories = new ArrayList<>();
}
