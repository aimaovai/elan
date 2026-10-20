package com.elan.repository;

import com.elan.entity.DiningExperience;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiningExperienceRepository extends JpaRepository<DiningExperience, UUID> {
    @EntityGraph(attributePaths = "courses")
    List<DiningExperience> findByStatusAndEndDateGreaterThanEqualOrderByDisplayOrderAsc(String status, LocalDate today);

    @EntityGraph(attributePaths = "courses")
    Optional<DiningExperience> findBySlugAndStatusAndEndDateGreaterThanEqual(String slug, String status, LocalDate today);

    @EntityGraph(attributePaths = "courses")
    Optional<DiningExperience> findFirstByFeaturedTrueAndStatusAndEndDateGreaterThanEqualOrderByDisplayOrderAsc(
            String status, LocalDate today);
}
