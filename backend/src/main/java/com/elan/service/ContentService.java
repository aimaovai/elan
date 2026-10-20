package com.elan.service;

import com.elan.dto.ApiDtos.*;
import com.elan.entity.*;
import com.elan.exception.ResourceNotFoundException;
import com.elan.repository.*;
import java.util.List;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContentService {
    private final MenuRepository menus;
    private final MenuItemRepository items;
    private final DiningExperienceRepository experiences;
    private final PrivateDiningSpaceRepository spaces;
    private final GalleryImageRepository gallery;
    private final RestaurantSettingsRepository settings;

    public ContentService(MenuRepository menus, MenuItemRepository items,
                          DiningExperienceRepository experiences, PrivateDiningSpaceRepository spaces,
                          GalleryImageRepository gallery, RestaurantSettingsRepository settings) {
        this.menus = menus;
        this.items = items;
        this.experiences = experiences;
        this.spaces = spaces;
        this.gallery = gallery;
        this.settings = settings;
    }

    public List<MenuDto> menus() {
        return menus.findByPublishedTrueOrderByDisplayOrderAsc().stream().map(this::toMenu).toList();
    }

    public MenuDto menu(String slug) {
        return menus.findBySlugAndPublishedTrue(slug).map(this::toMenu)
                .orElseThrow(() -> new ResourceNotFoundException("Menu", slug));
    }

    public List<MenuItemDto> featuredItems() {
        return items.findTop3ByFeaturedTrueAndAvailableTrueAndCategory_Menu_PublishedTrueOrderByDisplayOrderAsc()
                .stream().map(this::toItem).toList();
    }

    public List<ExperienceDto> experiences() {
        return experiences.findByStatusAndEndDateGreaterThanEqualOrderByDisplayOrderAsc("PUBLISHED", LocalDate.now())
                .stream().map(this::toExperience).toList();
    }

    public ExperienceDto experience(String slug) {
        return experiences.findBySlugAndStatusAndEndDateGreaterThanEqual(slug, "PUBLISHED", LocalDate.now())
                .map(this::toExperience)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", slug));
    }

    public ExperienceDto featuredExperience() {
        LocalDate today = LocalDate.now();
        return experiences.findFirstByFeaturedTrueAndStatusAndEndDateGreaterThanEqualOrderByDisplayOrderAsc(
                        "PUBLISHED", today)
                .map(this::toExperience)
                .orElseGet(() -> experiences.findByStatusAndEndDateGreaterThanEqualOrderByDisplayOrderAsc(
                                "PUBLISHED", today).stream()
                        .findFirst().map(this::toExperience)
                        .orElseThrow(() -> new ResourceNotFoundException("Featured experience", "current")));
    }

    public List<PrivateDiningSpaceDto> spaces() {
        return spaces.findByActiveTrueOrderByDisplayOrderAsc().stream().map(this::toSpace).toList();
    }

    public PrivateDiningSpaceDto space(String slug) {
        return spaces.findBySlugAndActiveTrue(slug).map(this::toSpace)
                .orElseThrow(() -> new ResourceNotFoundException("Private dining space", slug));
    }

    public List<GalleryImageDto> gallery(String category) {
        List<GalleryImage> images = category == null || category.isBlank()
                ? gallery.findByPublishedTrueOrderByDisplayOrderAsc()
                : gallery.findByPublishedTrueAndCategoryIgnoreCaseOrderByDisplayOrderAsc(category);
        return images.stream().map(this::toGalleryImage).toList();
    }

    public RestaurantDto restaurant() {
        RestaurantSettings value = settings.findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant information", "settings"));
        return new RestaurantDto(value.restaurantName, value.tagline, value.description, value.city,
                value.contactEmail, value.openingHours, value.reservationPolicy, value.dressCode);
    }

    private MenuDto toMenu(Menu value) {
        return new MenuDto(value.id, value.name, value.slug, value.description, value.menuType,
                value.validFrom, value.validUntil, value.categories.stream().map(category ->
                new MenuCategoryDto(category.id, category.name, category.slug, category.description,
                        category.items.stream().map(this::toItem).toList())).toList());
    }

    private MenuItemDto toItem(MenuItem value) {
        return new MenuItemDto(value.id, value.name, value.description, value.price, value.imageUrl,
                value.vegetarian, value.vegan, value.glutenFree, value.featured, value.available, value.seasonal,
                value.allergens.stream().map(allergen -> allergen.name).sorted().toList());
    }

    private ExperienceDto toExperience(DiningExperience value) {
        return new ExperienceDto(value.id, value.name, value.slug, value.shortDescription, value.fullDescription,
                value.heroImageUrl, value.price, value.winePairingPrice, value.courseCount,
                value.startDate, value.endDate, value.status, value.featured,
                value.courses.stream().map(course -> new ExperienceCourseDto(course.id, course.courseNumber,
                        course.name, course.description, course.imageUrl)).toList());
    }

    private PrivateDiningSpaceDto toSpace(PrivateDiningSpace value) {
        return new PrivateDiningSpaceDto(value.id, value.name, value.slug, value.description,
                value.capacity, value.imageUrl);
    }

    private GalleryImageDto toGalleryImage(GalleryImage value) {
        return new GalleryImageDto(value.id, value.title, value.altText, value.imageUrl, value.category);
    }
}
