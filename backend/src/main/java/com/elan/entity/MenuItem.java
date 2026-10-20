package com.elan.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "menu_items")
public class MenuItem {
    @Id public UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false) public MenuCategory category;
    @Column(nullable = false, length = 120) public String name;
    @Column(nullable = false, columnDefinition = "text") public String description;
    @Column(nullable = false, precision = 8, scale = 2) public BigDecimal price;
    @Column(name = "image_url", columnDefinition = "text") public String imageUrl;
    @Column(name = "is_vegetarian", nullable = false) public boolean vegetarian;
    @Column(name = "is_vegan", nullable = false) public boolean vegan;
    @Column(name = "is_gluten_free", nullable = false) public boolean glutenFree;
    @Column(name = "is_featured", nullable = false) public boolean featured;
    @Column(name = "is_available", nullable = false) public boolean available;
    @Column(name = "is_seasonal", nullable = false) public boolean seasonal;
    @Column(name = "display_order", nullable = false) public int displayOrder;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "menu_item_allergens",
            joinColumns = @JoinColumn(name = "menu_item_id"),
            inverseJoinColumns = @JoinColumn(name = "allergen_id"))
    public Set<Allergen> allergens = new LinkedHashSet<>();
}
