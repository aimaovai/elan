package com.elan.repository;

import com.elan.entity.MenuItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
    @EntityGraph(attributePaths = {"category", "category.menu", "allergens"})
    List<MenuItem> findTop3ByFeaturedTrueAndAvailableTrueAndCategory_Menu_PublishedTrueOrderByDisplayOrderAsc();
}
