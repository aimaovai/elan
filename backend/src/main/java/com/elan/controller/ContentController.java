package com.elan.controller;

import com.elan.dto.ApiDtos.*;
import com.elan.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ContentController {
    private final ContentService content;

    public ContentController(ContentService content) {
        this.content = content;
    }

    @GetMapping("/menus")
    @Operation(summary = "List published menus")
    public List<MenuDto> menus() { return content.menus(); }

    @GetMapping("/menus/featured-items")
    @Operation(summary = "List featured available menu items")
    public List<MenuItemDto> featuredItems() { return content.featuredItems(); }

    @GetMapping("/menus/{slug}")
    @Operation(summary = "Get a published menu by slug")
    public MenuDto menu(@PathVariable String slug) { return content.menu(slug); }

    @GetMapping("/experiences")
    @Operation(summary = "List published dining experiences")
    public List<ExperienceDto> experiences() { return content.experiences(); }

    @GetMapping("/experiences/featured")
    @Operation(summary = "Get the featured published dining experience")
    public ExperienceDto featuredExperience() { return content.featuredExperience(); }

    @GetMapping("/experiences/{slug}")
    @Operation(summary = "Get a published dining experience by slug")
    public ExperienceDto experience(@PathVariable String slug) { return content.experience(slug); }

    @GetMapping("/private-dining/spaces")
    @Operation(summary = "List active private dining spaces")
    public List<PrivateDiningSpaceDto> spaces() { return content.spaces(); }

    @GetMapping("/private-dining/spaces/{slug}")
    @Operation(summary = "Get an active private dining space by slug")
    public PrivateDiningSpaceDto space(@PathVariable String slug) { return content.space(slug); }

    @GetMapping("/gallery")
    @Operation(summary = "List published gallery images")
    public List<GalleryImageDto> gallery(@RequestParam(required = false) String category) {
        return content.gallery(category);
    }

    @GetMapping("/restaurant")
    @Operation(summary = "Get public restaurant information")
    public RestaurantDto restaurant() { return content.restaurant(); }
}
