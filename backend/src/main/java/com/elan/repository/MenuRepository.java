package com.elan.repository;

import com.elan.entity.Menu;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, UUID> {
    @EntityGraph(attributePaths = {"categories", "categories.items", "categories.items.allergens"})
    List<Menu> findByPublishedTrueOrderByDisplayOrderAsc();

    @EntityGraph(attributePaths = {"categories", "categories.items", "categories.items.allergens"})
    Optional<Menu> findBySlugAndPublishedTrue(String slug);
}
