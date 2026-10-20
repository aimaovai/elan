package com.elan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() {}

    public record MenuItemDto(UUID id, String name, String description, BigDecimal price, String imageUrl,
                              boolean vegetarian, boolean vegan, boolean glutenFree, boolean featured,
                              boolean available, boolean seasonal, List<String> allergens) {}
    public record MenuCategoryDto(UUID id, String name, String slug, String description, List<MenuItemDto> items) {}
    public record MenuDto(UUID id, String name, String slug, String description, String menuType,
                          LocalDate validFrom, LocalDate validUntil, List<MenuCategoryDto> categories) {}
    public record ExperienceCourseDto(UUID id, int courseNumber, String name, String description,
                                      String imageUrl) {}
    public record ExperienceDto(UUID id, String name, String slug, String shortDescription,
                                String fullDescription, String heroImageUrl, BigDecimal price,
                                BigDecimal winePairingPrice, int courseCount, LocalDate startDate,
                                LocalDate endDate, String status, boolean featured,
                                List<ExperienceCourseDto> courses) {}
    public record PrivateDiningSpaceDto(UUID id, String name, String slug, String description,
                                        int capacity, String imageUrl) {}
    public record GalleryImageDto(UUID id, String title, String altText, String imageUrl, String category) {}
    public record RestaurantDto(String restaurantName, String tagline, String description, String city,
                                String contactEmail, String openingHours, String reservationPolicy,
                                String dressCode) {}
}
