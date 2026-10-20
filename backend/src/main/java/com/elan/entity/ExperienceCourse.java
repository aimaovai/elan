package com.elan.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "experience_courses", uniqueConstraints = @UniqueConstraint(columnNames = {"experience_id", "course_number"}))
public class ExperienceCourse {
    @Id public UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experience_id", nullable = false) public DiningExperience experience;
    @Column(name = "course_number", nullable = false) public int courseNumber;
    @Column(nullable = false, length = 120) public String name;
    @Column(nullable = false, columnDefinition = "text") public String description;
    @Column(name = "image_url", columnDefinition = "text") public String imageUrl;
    @Column(name = "display_order", nullable = false) public int displayOrder;
}
