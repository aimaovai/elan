package com.elan.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "menu_categories")
public class MenuCategory {
    @Id public UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false) public Menu menu;
    @Column(nullable = false, length = 100) public String name;
    @Column(nullable = false, length = 100) public String slug;
    @Column(columnDefinition = "text") public String description;
    @Column(name = "display_order", nullable = false) public int displayOrder;
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    public List<MenuItem> items = new ArrayList<>();
}
