package com.elan.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dining_experiences")
public class DiningExperience {
    @Id public UUID id;
    @Column(nullable = false, length = 120) public String name;
    @Column(nullable = false, unique = true, length = 120) public String slug;
    @Column(name = "short_description", nullable = false, columnDefinition = "text") public String shortDescription;
    @Column(name = "full_description", nullable = false, columnDefinition = "text") public String fullDescription;
    @Column(name = "hero_image_url", nullable = false, columnDefinition = "text") public String heroImageUrl;
    @Column(nullable = false, precision = 8, scale = 2) public BigDecimal price;
    @Column(name = "wine_pairing_price", precision = 8, scale = 2) public BigDecimal winePairingPrice;
    @Column(name = "course_count", nullable = false) public int courseCount;
    @Column(name = "start_date", nullable = false) public LocalDate startDate;
    @Column(name = "end_date", nullable = false) public LocalDate endDate;
    @Column(nullable = false, length = 20) public String status;
    @Column(name = "is_featured", nullable = false) public boolean featured;
    @Column(name = "display_order", nullable = false) public int displayOrder;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
    @OneToMany(mappedBy = "experience", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    public List<ExperienceCourse> courses = new ArrayList<>();
}
