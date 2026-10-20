package com.elan.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.elan.entity.DiningExperience;
import com.elan.entity.Menu;
import com.elan.exception.ResourceNotFoundException;
import com.elan.repository.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentServiceTest {
    private MenuRepository menus;
    private MenuItemRepository items;
    private DiningExperienceRepository experiences;
    private PrivateDiningSpaceRepository spaces;
    private GalleryImageRepository gallery;
    private RestaurantSettingsRepository settings;
    private ContentService service;

    @BeforeEach
    void setUp() {
        menus = mock(MenuRepository.class);
        items = mock(MenuItemRepository.class);
        experiences = mock(DiningExperienceRepository.class);
        spaces = mock(PrivateDiningSpaceRepository.class);
        gallery = mock(GalleryImageRepository.class);
        settings = mock(RestaurantSettingsRepository.class);
        service = new ContentService(menus, items, experiences, spaces, gallery, settings);
    }

    @Test
    void mapsPublishedMenuAndItsCategories() {
        Menu menu = new Menu();
        menu.name = "Dinner";
        menu.slug = "dinner";
        menu.description = "Seasonal.";
        menu.menuType = "DINNER";
        when(menus.findByPublishedTrueOrderByDisplayOrderAsc()).thenReturn(List.of(menu));

        var result = service.menus();

        assertEquals(1, result.size());
        assertEquals("dinner", result.getFirst().slug());
        assertTrue(result.getFirst().categories().isEmpty());
    }

    @Test
    void unknownMenuSlugReturnsNotFound() {
        when(menus.findBySlugAndPublishedTrue("unknown")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.menu("unknown"));
    }

    @Test
    void selectsFeaturedPublishedExperience() {
        DiningExperience featured = new DiningExperience();
        featured.name = "Autumn Nocturne";
        featured.slug = "autumn-nocturne";
        featured.shortDescription = "An autumn tasting.";
        featured.fullDescription = "Seven courses.";
        featured.heroImageUrl = "https://example.test/hero.jpg";
        featured.price = java.math.BigDecimal.valueOf(185);
        featured.courseCount = 7;
        featured.startDate = LocalDate.parse("2026-09-01");
        featured.endDate = LocalDate.parse("2026-11-30");
        featured.status = "PUBLISHED";
        when(experiences.findFirstByFeaturedTrueAndStatusAndEndDateGreaterThanEqualOrderByDisplayOrderAsc(
                eq("PUBLISHED"), any(LocalDate.class)))
                .thenReturn(Optional.of(featured));

        assertEquals("autumn-nocturne", service.featuredExperience().slug());
    }

    @Test
    void appliesRequestedGalleryCategory() {
        when(gallery.findByPublishedTrueAndCategoryIgnoreCaseOrderByDisplayOrderAsc("Cuisine"))
                .thenReturn(List.of());

        assertTrue(service.gallery("Cuisine").isEmpty());
        verify(gallery).findByPublishedTrueAndCategoryIgnoreCaseOrderByDisplayOrderAsc("Cuisine");
    }
}
