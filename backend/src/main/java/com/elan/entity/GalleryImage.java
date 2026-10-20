package com.elan.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "gallery_images")
public class GalleryImage {
    @Id public UUID id;
    @Column(nullable = false, length = 120) public String title;
    @Column(name = "alt_text", nullable = false, columnDefinition = "text") public String altText;
    @Column(name = "image_url", nullable = false, columnDefinition = "text") public String imageUrl;
    @Column(nullable = false, length = 50) public String category;
    @Column(name = "display_order", nullable = false) public int displayOrder;
    @Column(name = "is_published", nullable = false) public boolean published;
}
