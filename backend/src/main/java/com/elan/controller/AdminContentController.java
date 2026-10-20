package com.elan.controller;

import com.elan.entity.*;
import com.elan.exception.BusinessRuleException;
import com.elan.exception.ResourceNotFoundException;
import com.elan.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminContentController {
    private static final Set<String> ACTIVE_BOOKINGS = Set.of("PENDING", "CONFIRMED", "SEATED");
    private final MenuRepository menus;
    private final MenuItemRepository items;
    private final DiningExperienceRepository experiences;
    private final ReservationRepository reservations;
    private final AllergenRepository allergens;

    public AdminContentController(MenuRepository menus, MenuItemRepository items,
            DiningExperienceRepository experiences, ReservationRepository reservations, AllergenRepository allergens) {
        this.menus = menus;
        this.items = items;
        this.experiences = experiences;
        this.reservations = reservations;
        this.allergens = allergens;
    }

    @GetMapping("/menus")
    @Transactional(readOnly = true)
    public List<MenuView> menus() {
        return menus.findAll().stream().sorted(Comparator.comparingInt(value -> value.displayOrder))
                .map(this::menuView).toList();
    }

    @PostMapping("/menus")
    @Transactional
    public MenuView createMenu(@Valid @RequestBody MenuUpdate input) {
        Menu menu = new Menu();
        menu.id = UUID.randomUUID();
        menu.createdAt = LocalDateTime.now();
        menu.published = false;
        applyMenu(menu, input);
        menus.save(menu);
        return menuView(menu);
    }

    @PutMapping("/menus/{id}")
    @Transactional
    public MenuView updateMenu(@PathVariable UUID id, @Valid @RequestBody MenuUpdate input) {
        Menu menu = menus.findById(id).orElseThrow(() -> new ResourceNotFoundException("Menu", id.toString()));
        applyMenu(menu, input);
        menu.updatedAt = LocalDateTime.now();
        menus.save(menu);
        return menuView(menu);
    }

    @PatchMapping("/menus/{id}/publication")
    @Transactional
    public MenuView publishMenu(@PathVariable UUID id, @Valid @RequestBody PublicationUpdate update) {
        Menu menu = menus.findById(id).orElseThrow(() -> new ResourceNotFoundException("Menu", id.toString()));
        menu.published = update.published();
        menu.updatedAt = LocalDateTime.now();
        menus.save(menu);
        return menuView(menu);
    }

    @PostMapping("/menus/{id}/categories")
    @Transactional
    public CategoryView createCategory(@PathVariable UUID id, @Valid @RequestBody CategoryUpdate update) {
        Menu menu = menus.findById(id).orElseThrow(() -> new ResourceNotFoundException("Menu", id.toString()));
        MenuCategory category = new MenuCategory();
        category.id = UUID.randomUUID();
        category.menu = menu;
        applyCategory(category, update);
        menu.categories.add(category);
        menu.updatedAt = LocalDateTime.now();
        menus.save(menu);
        return categoryView(category);
    }

    @PutMapping("/menus/{menuId}/categories/{categoryId}")
    @Transactional
    public CategoryView updateCategory(@PathVariable UUID menuId, @PathVariable UUID categoryId,
                                       @Valid @RequestBody CategoryUpdate update) {
        Menu menu = menus.findById(menuId).orElseThrow(() -> new ResourceNotFoundException("Menu", menuId.toString()));
        MenuCategory category = menu.categories.stream().filter(c -> c.id.equals(categoryId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Menu category", categoryId.toString()));
        applyCategory(category, update);
        menu.updatedAt = LocalDateTime.now();
        menus.save(menu);
        return categoryView(category);
    }

    @PostMapping("/menus/{menuId}/categories/{categoryId}/items")
    @Transactional
    public ItemView createMenuItem(@PathVariable UUID menuId, @PathVariable UUID categoryId,
                                   @Valid @RequestBody ItemUpdate update) {
        Menu menu = menus.findById(menuId).orElseThrow(() -> new ResourceNotFoundException("Menu", menuId.toString()));
        MenuCategory category = menu.categories.stream().filter(c -> c.id.equals(categoryId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Menu category", categoryId.toString()));
        MenuItem item = new MenuItem();
        item.id = UUID.randomUUID();
        item.category = category;
        item.createdAt = LocalDateTime.now();
        applyItem(item, update);
        category.items.add(item);
        menu.updatedAt = LocalDateTime.now();
        menus.save(menu);
        return itemView(item);
    }

    @PutMapping("/menu-items/{id}")
    @Transactional
    public ItemView updateMenuItem(@PathVariable UUID id, @Valid @RequestBody ItemUpdate update) {
        MenuItem item = items.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", id.toString()));
        applyItem(item, update);
        item.updatedAt = LocalDateTime.now();
        items.save(item);
        return itemView(item);
    }

    @GetMapping("/experiences")
    @Transactional(readOnly = true)
    public List<ExperienceView> experiences() {
        return experiences.findAll().stream().sorted(Comparator.comparingInt(value -> value.displayOrder))
                .map(this::experienceView).toList();
    }

    @PostMapping("/experiences")
    @Transactional
    public ExperienceView createExperience(@Valid @RequestBody ExperienceUpdate input) {
        if (!input.endDate().isAfter(input.startDate()))
            throw new BusinessRuleException("EXPERIENCE_INVALID_DATES", "End date must be after start date.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        DiningExperience experience = new DiningExperience();
        experience.id = UUID.randomUUID();
        experience.createdAt = LocalDateTime.now();
        experience.status = "DRAFT";
        applyExperience(experience, input);
        experiences.save(experience);
        return experienceView(experience);
    }

    @PutMapping("/experiences/{id}")
    @Transactional
    public ExperienceView updateExperience(@PathVariable UUID id, @Valid @RequestBody ExperienceUpdate input) {
        DiningExperience experience = experiences.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", id.toString()));
        if (!input.endDate().isAfter(input.startDate()))
            throw new BusinessRuleException("EXPERIENCE_INVALID_DATES", "End date must be after start date.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        boolean impactsBookings = !experience.startDate.equals(input.startDate())
                || !experience.endDate.equals(input.endDate())
                || experience.reservationDurationMinutes != input.reservationDurationMinutes();
        if (impactsBookings && hasFutureBookings(experience.id))
            throw new BusinessRuleException("EXPERIENCE_HAS_FUTURE_RESERVATIONS",
                    "Dates or duration cannot change while future reservations use this experience.",
                    HttpStatus.CONFLICT);
        applyExperience(experience, input);
        experience.updatedAt = LocalDateTime.now();
        experiences.save(experience);
        return experienceView(experience);
    }

    @PatchMapping("/experiences/{id}/publication")
    @Transactional
    public ExperienceView publishExperience(@PathVariable UUID id, @Valid @RequestBody ExperiencePublication update) {
        DiningExperience experience = experiences.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", id.toString()));
        if (!"PUBLISHED".equals(update.status()) && hasFutureBookings(experience.id))
            throw new BusinessRuleException("EXPERIENCE_HAS_FUTURE_RESERVATIONS",
                    "This experience cannot be unpublished while future reservations use it.", HttpStatus.CONFLICT);
        experience.status = update.status();
        experience.updatedAt = LocalDateTime.now();
        experiences.save(experience);
        return experienceView(experience);
    }

    @PutMapping("/experiences/{id}/courses")
    @Transactional
    public ExperienceView updateCourses(@PathVariable UUID id, @Valid @RequestBody CourseListUpdate update) {
        DiningExperience experience = experiences.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", id.toString()));
        Map<UUID, ExperienceCourse> existing = new HashMap<>();
        experience.courses.forEach(course -> existing.put(course.id, course));
        int order = 0;
        for (CourseUpdate input : update.courses()) {
            ExperienceCourse course;
            if (input.id() == null) {
                course = new ExperienceCourse();
                course.id = UUID.randomUUID();
                course.experience = experience;
                experience.courses.add(course);
            } else {
                course = existing.get(input.id());
                if (course == null) throw new BusinessRuleException("COURSE_NOT_IN_EXPERIENCE",
                        "A course does not belong to this experience.", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            course.courseNumber = order + 1;
            course.displayOrder = order++;
            course.name = input.name().trim();
            course.description = input.description().trim();
            course.imageUrl = input.imageUrl();
        }
        experience.courseCount = experience.courses.size();
        experience.updatedAt = LocalDateTime.now();
        experiences.save(experience);
        return experienceView(experience);
    }

    private void applyMenu(Menu menu, MenuUpdate input) {
        menu.name = input.name().trim();
        menu.slug = input.slug().trim().toLowerCase(Locale.ROOT);
        menu.description = input.description().trim();
        menu.menuType = input.menuType().trim().toUpperCase(Locale.ROOT);
        menu.validFrom = input.validFrom();
        menu.validUntil = input.validUntil();
        menu.displayOrder = input.displayOrder();
        if (menu.createdAt == null) menu.createdAt = LocalDateTime.now();
        menu.updatedAt = LocalDateTime.now();
    }
    private void applyCategory(MenuCategory category, CategoryUpdate input) {
        category.name = input.name().trim();
        category.slug = input.slug().trim().toLowerCase(Locale.ROOT);
        category.description = input.description();
        category.displayOrder = input.displayOrder();
    }
    private void applyItem(MenuItem item, ItemUpdate input) {
        item.name = input.name().trim();
        item.description = input.description().trim();
        item.price = input.price();
        item.imageUrl = input.imageUrl();
        item.vegetarian = input.vegetarian();
        item.vegan = input.vegan();
        item.glutenFree = input.glutenFree();
        item.featured = input.featured();
        item.available = input.available();
        item.seasonal = input.seasonal();
        item.displayOrder = input.displayOrder();
        Set<Allergen> selected = new LinkedHashSet<>();
        for (String name : input.allergens()) {
            Allergen allergen = allergens.findByNameIgnoreCase(name.trim())
                    .orElseThrow(() -> new BusinessRuleException("UNKNOWN_ALLERGEN",
                            "Choose an allergen from the existing list.", HttpStatus.UNPROCESSABLE_ENTITY));
            selected.add(allergen);
        }
        item.allergens = selected;
    }
    private void applyExperience(DiningExperience e, ExperienceUpdate input) {
        e.name = input.name().trim();
        e.slug = input.slug().trim().toLowerCase(Locale.ROOT);
        e.shortDescription = input.shortDescription().trim();
        e.fullDescription = input.fullDescription().trim();
        e.heroImageUrl = input.heroImageUrl().trim();
        e.price = input.price();
        e.winePairingPrice = input.winePairingPrice();
        e.reservationDurationMinutes = input.reservationDurationMinutes();
        e.startDate = input.startDate();
        e.endDate = input.endDate();
        e.featured = input.featured();
        e.displayOrder = input.displayOrder();
        e.courseCount = e.courses.size();
        if (e.createdAt == null) e.createdAt = LocalDateTime.now();
        e.updatedAt = LocalDateTime.now();
    }
    private boolean hasFutureBookings(UUID id) {
        return reservations.countByExperience_IdAndReservationDateGreaterThanEqualAndStatusIn(
                id, LocalDate.now(), ACTIVE_BOOKINGS) > 0;
    }
    private MenuView menuView(Menu menu) {
        return new MenuView(menu.id, menu.name, menu.slug, menu.description, menu.menuType, menu.published,
                menu.validFrom, menu.validUntil, menu.displayOrder, menu.categories.stream()
                .sorted(Comparator.comparingInt(c -> c.displayOrder)).map(this::categoryView).toList());
    }
    private CategoryView categoryView(MenuCategory category) {
        return new CategoryView(category.id, category.name, category.slug, category.description,
                category.displayOrder, category.items.stream().sorted(Comparator.comparingInt(i -> i.displayOrder))
                .map(this::itemView).toList());
    }
    private ItemView itemView(MenuItem item) {
        return new ItemView(item.id, item.name, item.description, item.price, item.imageUrl, item.vegetarian,
                item.vegan, item.glutenFree, item.featured, item.available, item.seasonal, item.displayOrder,
                item.allergens.stream().map(allergen -> allergen.name).sorted().toList());
    }
    private ExperienceView experienceView(DiningExperience e) {
        return new ExperienceView(e.id, e.name, e.slug, e.shortDescription, e.fullDescription, e.heroImageUrl,
                e.price, e.winePairingPrice, e.status, e.featured, e.startDate, e.endDate,
                e.reservationDurationMinutes, e.courseCount, e.displayOrder, e.courses.stream()
                .sorted(Comparator.comparingInt(c -> c.displayOrder))
                .map(c -> new CourseView(c.id, c.courseNumber, c.name, c.description, c.imageUrl, c.displayOrder)).toList());
    }

    public record MenuView(UUID id, String name, String slug, String description, String menuType, boolean published,
            LocalDate validFrom, LocalDate validUntil, int displayOrder, List<CategoryView> categories) {}
    public record CategoryView(UUID id, String name, String slug, String description, int displayOrder, List<ItemView> items) {}
    public record ItemView(UUID id, String name, String description, BigDecimal price, String imageUrl,
            boolean vegetarian, boolean vegan, boolean glutenFree, boolean featured, boolean available,
            boolean seasonal, int displayOrder, List<String> allergens) {}
    public record ExperienceView(UUID id, String name, String slug, String shortDescription, String fullDescription,
            String heroImageUrl, BigDecimal price, BigDecimal winePairingPrice, String status, boolean featured,
            LocalDate startDate, LocalDate endDate, int reservationDurationMinutes, int courseCount,
            int displayOrder, List<CourseView> courses) {}
    public record CourseView(UUID id, int courseNumber, String name, String description, String imageUrl, int displayOrder) {}
    public record MenuUpdate(@NotBlank @Size(max = 100) String name, @NotBlank @Size(max = 100) String slug,
            @NotBlank @Size(max = 5000) String description, @NotBlank @Size(max = 30) String menuType,
            LocalDate validFrom, LocalDate validUntil, @Min(0) int displayOrder) {}
    public record CategoryUpdate(@NotBlank @Size(max = 100) String name, @NotBlank @Size(max = 100) String slug,
            @Size(max = 1000) String description, @Min(0) int displayOrder) {}
    public record ItemUpdate(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 4000) String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) BigDecimal price,
            @Size(max = 2000) String imageUrl, boolean vegetarian, boolean vegan, boolean glutenFree,
            boolean featured, boolean available, boolean seasonal, @Min(0) int displayOrder,
            @NotNull @Size(max = 30) List<@NotBlank @Size(max = 50) String> allergens) {}
    public record ExperienceUpdate(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 120) String slug,
            @NotBlank @Size(max = 2000) String shortDescription, @NotBlank @Size(max = 8000) String fullDescription,
            @NotBlank @Size(max = 2000) String heroImageUrl, @NotNull @DecimalMin("0.00")
            @Digits(integer = 6, fraction = 2) BigDecimal price, @DecimalMin("0.00")
            @Digits(integer = 6, fraction = 2) BigDecimal winePairingPrice, @NotNull LocalDate startDate,
            @NotNull LocalDate endDate, @Min(30) @Max(600) int reservationDurationMinutes,
            boolean featured, @Min(0) int displayOrder) {}
    public record PublicationUpdate(boolean published) {}
    public record ExperiencePublication(@Pattern(regexp = "PUBLISHED|DRAFT|ARCHIVED") String status) {}
    public record CourseListUpdate(@NotNull @Size(max = 20) List<@Valid CourseUpdate> courses) {}
    public record CourseUpdate(UUID id, @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 4000) String description, @Size(max = 2000) String imageUrl) {}
}
