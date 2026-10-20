package com.elan.repository;

import com.elan.entity.GalleryImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryImageRepository extends JpaRepository<GalleryImage, UUID> {
    List<GalleryImage> findByPublishedTrueOrderByDisplayOrderAsc();
    List<GalleryImage> findByPublishedTrueAndCategoryIgnoreCaseOrderByDisplayOrderAsc(String category);
}
