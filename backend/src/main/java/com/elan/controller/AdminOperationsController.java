package com.elan.controller;

import com.elan.dto.ReservationDtos.ReservationRequest;
import com.elan.dto.ReservationDtos.ReservationResponse;
import com.elan.dto.ReservationDtos.ReservationChanges;
import com.elan.entity.*;
import com.elan.exception.BusinessRuleException;
import com.elan.exception.ResourceNotFoundException;
import com.elan.repository.*;
import com.elan.service.ReservationService;
import com.elan.notification.NotificationService;
import com.elan.repository.AdministratorRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/admin")
public class AdminOperationsController {
    private static final Set<String> ACTIVE_STATUSES = Set.of("PENDING", "CONFIRMED", "SEATED");
    private final ReservationRepository reservations;
    private final ReservationStatusHistoryRepository history;
    private final DiningTableRepository tables;
    private final PrivateDiningInquiryRepository inquiries;
    private final RestaurantSettingsRepository settings;
    private final RestaurantServicePeriodRepository servicePeriods;
    private final RestaurantBlockedDateRepository blockedDates;
    private final AdministrativeAuditLogRepository audit;
    private final AdministratorRepository administrators;
    private final NotificationOutboxRepository notifications;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final MenuItemRepository menuItems;

    public AdminOperationsController(ReservationRepository reservations, ReservationStatusHistoryRepository history,
            DiningTableRepository tables, PrivateDiningInquiryRepository inquiries,
            RestaurantSettingsRepository settings, RestaurantServicePeriodRepository servicePeriods,
            RestaurantBlockedDateRepository blockedDates, AdministrativeAuditLogRepository audit,
            AdministratorRepository administrators, NotificationOutboxRepository notifications,
            ReservationService reservationService, NotificationService notificationService,
            MenuItemRepository menuItems) {
        this.reservations = reservations;
        this.history = history;
        this.tables = tables;
        this.inquiries = inquiries;
        this.settings = settings;
        this.servicePeriods = servicePeriods;
        this.blockedDates = blockedDates;
        this.audit = audit;
        this.administrators = administrators;
        this.notifications = notifications;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
        this.menuItems = menuItems;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','DEMO_VIEWER')")
    @Transactional(readOnly = true)
    public Dashboard dashboard(Authentication authentication) {
        LocalDate today = LocalDate.now(java.time.ZoneId.of(currentSettings().restaurantTimezone));
        boolean demo = isDemoViewer(authentication);
        List<Reservation> todays = reservations.findByReservationDateOrderByStartTimeAsc(today);
        List<ReservationSummary> todaySummary = todays.stream().map(r -> summary(r, demo)).toList();
        List<ReservationSummary> upcoming = reservations
                .findTop10ByReservationDateGreaterThanEqualOrderByReservationDateAscStartTimeAsc(today.plusDays(1))
                .stream().map(r -> summary(r, demo)).toList();
        List<InquirySummary> newInquiries = demo ? List.of() : inquiries.searchAdmin("NEW", null, "",
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream().map(this::inquirySummary).toList();
        long inquiryCount = inquiries.countByStatus("NEW");
        List<OperationalAlert> alerts = new ArrayList<>();
        for (RestaurantBlockedDate closure : blockedDates.findByBlockedDate(today)) {
            alerts.add(new OperationalAlert("CLOSURE", closure.fullDay ? "Restaurant closed today" : "Partial closure today",
                    closure.reason + (closure.fullDay ? "" : " · " + closure.startTime + "–" + closure.endTime),
                    "/admin/settings"));
        }
        if (inquiryCount > 0) alerts.add(new OperationalAlert("INQUIRIES", "Private dining inquiries need review",
                inquiryCount + " new private dining " + (inquiryCount == 1 ? "inquiry" : "inquiries"),
                "/admin/private-dining"));
        long unavailableItems = menuItems.countUnavailablePublishedItems();
        if (unavailableItems > 0) alerts.add(new OperationalAlert("MENU", "Unavailable published menu items",
                unavailableItems + " item" + (unavailableItems == 1 ? "" : "s") + " are marked unavailable.",
                "/admin/menus"));
        long guests = todays.stream().filter(r -> ACTIVE_STATUSES.contains(r.status)).mapToLong(r -> r.partySize).sum();
        return new Dashboard(reservations.countByReservationDate(today),
                reservations.countByReservationDateAndStatus(today, "CONFIRMED"), guests,
                reservations.countByStatusAndReservationDateGreaterThanEqual("CONFIRMED", today.plusDays(1)),
                inquiryCount, reservations.countByReservationDateAndStatus(today, "CANCELLED"),
                todaySummary, upcoming, newInquiries, alerts);
    }

    @GetMapping("/reservations")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','DEMO_VIEWER')")
    @Transactional(readOnly = true)
    public PageView<ReservationSummary> listReservations(
            @RequestParam(required = false) LocalDate date, @RequestParam(required = false) String status,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "reservationDate") String sort, Authentication authentication) {
        validateReservationStatus(status);
        String sortField = Set.of("reservationDate", "startTime", "partySize", "status", "confirmationCode")
                .contains(sort) ? sort : "reservationDate";
        Page<Reservation> result = reservations.searchAdmin(date, blankToNull(status), blankToEmpty(q),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, sortField).and(
                        Sort.by(Sort.Direction.ASC, "startTime"))));
        boolean demo = isDemoViewer(authentication);
        return PageView.from(result.map(r -> summary(r, demo)));
    }

    @GetMapping("/reservations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','DEMO_VIEWER')")
    @Transactional(readOnly = true)
    public ReservationDetail reservation(@PathVariable UUID id, Authentication authentication) {
        Reservation r = loadReservation(id);
        boolean demo = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DEMO_VIEWER"));
        GuestView guest = demo ? new GuestView("Guest", "", "", "")
                : new GuestView(r.guest.firstName, r.guest.lastName, r.guest.email, r.guest.phoneNumber);
        return new ReservationDetail(r.id, r.confirmationCode, guest, r.reservationDate, r.startTime, r.endTime,
                r.partySize, r.table.displayName, r.experience == null ? null : r.experience.name, r.status,
                r.specialOccasion, demo ? null : r.dietaryNotes, demo ? null : r.specialRequests,
                demo ? null : r.internalNotes,
                history.findByReservation_IdOrderByChangedAtDesc(r.id).stream()
                        .map(h -> new StatusHistory(h.oldStatus, h.newStatus, h.changeSource, h.administratorId,
                                demo ? null : h.reason, h.changedAt)).toList());
    }

    @PostMapping("/reservations")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Transactional
    public ReservationResponse createReservation(@RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReservationRequest request, Authentication authentication) {
        UUID adminId = administratorId(authentication);
        ReservationResponse created = reservationService.createForAdministrator(request, idempotencyKey, adminId);
        reservations.findByConfirmationCode(created.confirmationCode()).ifPresent(reservation ->
                recordAudit(adminId, "RESERVATION", reservation.id, "CREATED", null,
                        reservation.confirmationCode, "Created by administrator."));
        return created;
    }

    @PatchMapping("/reservations/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Transactional
    public ReservationDetail updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusUpdate update,
                                          Authentication authentication) {
        Reservation reservation = loadReservation(id);
        String old = reservation.status;
        if (!validTransition(old, update.status()))
            throw new BusinessRuleException("RESERVATION_INVALID_STATE_TRANSITION",
                    "This reservation cannot transition from " + old + " to " + update.status() + ".",
                    HttpStatus.CONFLICT);
        reservation.status = update.status();
        reservation.updatedAt = LocalDateTime.now();
        if ("CANCELLED".equals(update.status())) {
            reservation.cancelledAt = reservation.updatedAt;
            reservation.cancellationReason = trimToNull(update.reason());
        }
        reservations.save(reservation);
        ReservationStatusHistory event = new ReservationStatusHistory();
        event.id = UUID.randomUUID();
        event.reservation = reservation;
        event.oldStatus = old;
        event.newStatus = reservation.status;
        event.changeSource = "ADMINISTRATOR";
        event.administratorId = administratorId(authentication);
        event.reason = trimToNull(update.reason());
        event.changedAt = reservation.updatedAt;
        history.save(event);
        recordAudit(event.administratorId, "RESERVATION", id, "STATUS", old, reservation.status, event.reason);
        if ("CANCELLED".equals(reservation.status))
            notificationService.reservation(reservation, "RESERVATION_CANCELLATION", null);
        return reservation(id, authentication);
    }

    @PatchMapping("/reservations/{id}/modify")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Transactional
    public ReservationDetail modifyReservation(@PathVariable UUID id,
            @Valid @RequestBody AdminReservationChanges changes, Authentication authentication) {
        Reservation reservation = loadReservation(id);
        String old = reservation.reservationDate + ":" + reservation.startTime + ":" + reservation.partySize
                + ":" + reservation.table.displayName;
        reservationService.modifyByAdministrator(id, new ReservationChanges(changes.reservationDate(),
                changes.reservationTime(), changes.partySize(), changes.dietaryNotes(), changes.specialRequests()));
        Reservation updated = loadReservation(id);
        String value = updated.reservationDate + ":" + updated.startTime + ":" + updated.partySize
                + ":" + updated.table.displayName;
        recordAudit(administratorId(authentication), "RESERVATION", id, "MODIFIED", old, value,
                trimToNull(changes.reason()));
        return reservation(id, authentication);
    }

    @PatchMapping("/reservations/{id}/table")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Transactional
    public ReservationDetail reassignReservationTable(@PathVariable UUID id,
            @Valid @RequestBody TableAssignment assignment, Authentication authentication) {
        Reservation reservation = loadReservation(id);
        String oldTable = reservation.table.displayName;
        reservationService.reassignTableByAdministrator(id, assignment.tableId());
        Reservation updated = loadReservation(id);
        recordAudit(administratorId(authentication), "RESERVATION", id, "TABLE_REASSIGNED", oldTable,
                updated.table.displayName, trimToNull(assignment.reason()));
        notificationService.reservation(updated, "RESERVATION_MODIFICATION", null);
        return reservation(id, authentication);
    }

    @PatchMapping("/reservations/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Transactional
    public ReservationDetail updateNotes(@PathVariable UUID id, @Valid @RequestBody NotesUpdate update,
                                         Authentication authentication) {
        Reservation reservation = loadReservation(id);
        reservation.internalNotes = trimToNull(update.internalNotes());
        reservation.updatedAt = LocalDateTime.now();
        reservations.save(reservation);
        recordAudit(administratorId(authentication), "RESERVATION", id, "NOTES", null, "updated", null);
        return reservation(id, authentication);
    }

    @GetMapping("/tables")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','DEMO_VIEWER')")
    @Transactional(readOnly = true)
    public List<TableView> listTables() {
        return tables.findAll(Sort.by("tableNumber")).stream().map(this::tableView).toList();
    }

    @GetMapping("/tables/{id}/reservations")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','DEMO_VIEWER')")
    @Transactional(readOnly = true)
    public List<ReservationSummary> upcomingTableReservations(@PathVariable UUID id,
            @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days, Authentication authentication) {
        if (!tables.existsById(id)) throw new ResourceNotFoundException("Dining table", id.toString());
        LocalDate today = LocalDate.now(currentZone());
        return tables.findUpcomingReservations(id, today, ACTIVE_STATUSES).stream()
                .filter(reservation -> !reservation.reservationDate.isAfter(today.plusDays(days)))
                .sorted(Comparator.comparing((Reservation reservation) -> reservation.reservationDate)
                        .thenComparing(reservation -> reservation.startTime))
                .map(reservation -> summary(reservation, isDemoViewer(authentication))).toList();
    }

    @PostMapping("/tables")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public TableView createTable(@Valid @RequestBody TableUpdate input, Authentication authentication) {
        validateTableCapacity(input);
        if (tables.findByTableNumberIgnoreCase(input.tableNumber()).isPresent())
            throw new BusinessRuleException("TABLE_NUMBER_EXISTS", "That table number is already in use.", HttpStatus.CONFLICT);
        DiningTable table = new DiningTable();
        table.id = UUID.randomUUID();
        table.createdAt = LocalDateTime.now();
        applyTable(table, input);
        tables.save(table);
        recordAudit(administratorId(authentication), "TABLE", table.id, "CREATED", null, table.tableNumber, null);
        return tableView(table);
    }

    @PutMapping("/tables/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public TableView updateTable(@PathVariable UUID id, @Valid @RequestBody TableUpdate input,
                                 Authentication authentication) {
        DiningTable table = tables.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dining table", id.toString()));
        validateTableCapacity(input);
        if (tables.findByTableNumberIgnoreCase(input.tableNumber()).filter(other -> !other.id.equals(id)).isPresent())
            throw new BusinessRuleException("TABLE_NUMBER_EXISTS", "That table number is already in use.", HttpStatus.CONFLICT);
        List<Reservation> activeBookings = tables.findUpcomingReservations(
                table.id, LocalDate.now(currentZone()), ACTIVE_STATUSES);
        if (!input.active() && activeBookings.stream().anyMatch(r -> ACTIVE_STATUSES.contains(r.status)))
            throw new BusinessRuleException("TABLE_HAS_UPCOMING_RESERVATIONS",
                    "A table with upcoming reservations cannot be deactivated.", HttpStatus.CONFLICT);
        if (activeBookings.stream().anyMatch(r -> r.partySize > input.maximumCapacity()
                || r.partySize < input.minimumCapacity()))
            throw new BusinessRuleException("TABLE_CHANGE_CONFLICT",
                    "The new capacity conflicts with an upcoming reservation.", HttpStatus.CONFLICT);
        String old = table.tableNumber + ":" + table.maximumCapacity + ":" + table.active;
        applyTable(table, input);
        table.updatedAt = LocalDateTime.now();
        tables.save(table);
        recordAudit(administratorId(authentication), "TABLE", id, "UPDATED", old,
                table.tableNumber + ":" + table.maximumCapacity + ":" + table.active, null);
        return tableView(table);
    }

    @GetMapping("/private-dining/inquiries")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Transactional(readOnly = true)
    public PageView<InquirySummary> listInquiries(@RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate date, @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        validateInquiryStatus(status);
        Page<PrivateDiningInquiry> result = inquiries.searchAdmin(blankToNull(status), date, blankToEmpty(q),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageView.from(result.map(this::inquirySummary));
    }

    @GetMapping("/private-dining/inquiries/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Transactional(readOnly = true)
    public InquiryDetail inquiry(@PathVariable UUID id) {
        PrivateDiningInquiry i = inquiries.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Private dining inquiry", id.toString()));
        return new InquiryDetail(i.id, i.referenceNumber, i.firstName, i.lastName, i.email, i.phoneNumber,
                i.eventType, i.preferredDate, i.alternateDate, i.guestCount,
                i.space == null ? null : i.space.name, i.budgetPerGuest, i.dietaryRequirements,
                i.beveragePreferences, i.eventDescription, i.specialRequests, i.preferredContactMethod,
                i.status, i.internalNotes, i.createdAt);
    }

    @PatchMapping("/private-dining/inquiries/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Transactional
    public InquiryDetail updateInquiry(@PathVariable UUID id, @Valid @RequestBody InquiryUpdate update,
                                       Authentication authentication) {
        PrivateDiningInquiry inquiry = inquiries.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Private dining inquiry", id.toString()));
        String oldStatus = inquiry.status;
        if (!oldStatus.equals(update.status()) && !validInquiryTransition(oldStatus, update.status()))
            throw new BusinessRuleException("INQUIRY_INVALID_STATE_TRANSITION",
                    "This inquiry cannot transition from " + oldStatus + " to " + update.status() + ".",
                    HttpStatus.CONFLICT);
        inquiry.status = update.status();
        inquiry.internalNotes = trimToNull(update.internalNotes());
        inquiry.updatedAt = LocalDateTime.now();
        inquiries.save(inquiry);
        recordAudit(administratorId(authentication), "PRIVATE_DINING_INQUIRY", id, "UPDATED",
                oldStatus, inquiry.status, null);
        return inquiry(id);
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional(readOnly = true)
    public SettingsView getSettings() {
        RestaurantSettings value = settings.findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant settings", "settings"));
        return settingsView(value);
    }

    @PatchMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public SettingsView updateSettings(@Valid @RequestBody SettingsUpdate update, Authentication authentication) {
        RestaurantSettings value = settings.findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant settings", "settings"));
        if (update.maximumOnlinePartySize() < update.minimumPartySize())
            throw new BusinessRuleException("SETTINGS_INVALID_PARTY_SIZE",
                    "Maximum party size must be greater than or equal to minimum party size.", HttpStatus.UNPROCESSABLE_ENTITY);
        try {
            java.time.ZoneId.of(update.restaurantTimezone());
        } catch (java.time.DateTimeException exception) {
            throw new BusinessRuleException("SETTINGS_INVALID_TIMEZONE", "Choose a valid restaurant timezone.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        String old = value.restaurantTimezone + ":" + value.bookingEnabled + ":" + value.maximumOnlinePartySize;
        List<Reservation> futureBookings = reservations.findByReservationDateGreaterThanEqualAndStatusIn(
                LocalDate.now(currentZone()), ACTIVE_STATUSES);
        boolean settingsConflict = !value.restaurantTimezone.equals(update.restaurantTimezone())
                && !futureBookings.isEmpty()
                || futureBookings.stream().anyMatch(reservation -> reservation.partySize < update.minimumPartySize()
                || reservation.partySize > update.maximumOnlinePartySize()
                || reservation.reservationDate.isAfter(LocalDate.now(java.time.ZoneId.of(update.restaurantTimezone))
                .plusDays(update.maximumAdvanceBookingDays())));
        if (settingsConflict)
            throw new BusinessRuleException("SETTINGS_CONFLICTS_WITH_RESERVATIONS",
                    "These settings would invalidate one or more upcoming reservations.", HttpStatus.CONFLICT);
        value.restaurantTimezone = update.restaurantTimezone();
        value.minimumAdvanceBookingHours = update.minimumAdvanceBookingHours();
        value.maximumAdvanceBookingDays = update.maximumAdvanceBookingDays();
        value.minimumPartySize = update.minimumPartySize();
        value.maximumOnlinePartySize = update.maximumOnlinePartySize();
        value.defaultReservationDurationMinutes = update.defaultReservationDurationMinutes();
        value.reservationIntervalMinutes = update.reservationIntervalMinutes();
        value.cancellationCutoffHours = update.cancellationCutoffHours();
        value.bookingEnabled = update.bookingEnabled();
        value.privateDiningEnabled = update.privateDiningEnabled();
        value.updatedAt = LocalDateTime.now();
        settings.save(value);
        recordAudit(administratorId(authentication), "SETTINGS", value.id, "UPDATED", old,
                value.restaurantTimezone + ":" + value.bookingEnabled + ":" + value.maximumOnlinePartySize, null);
        return settingsView(value);
    }

    @GetMapping("/settings/service-periods")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional(readOnly = true)
    public List<ServicePeriodView> servicePeriodList() {
        return servicePeriods.findAll(Sort.by("dayOfWeek").and(Sort.by("startTime")))
                .stream().map(this::servicePeriodView).toList();
    }

    @PostMapping("/settings/service-periods")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ServicePeriodView createServicePeriod(@Valid @RequestBody ServicePeriodUpdate input,
                                                  Authentication authentication) {
        validateServicePeriod(input, null);
        RestaurantServicePeriod period = new RestaurantServicePeriod();
        period.id = UUID.randomUUID();
        applyServicePeriod(period, input);
        servicePeriods.save(period);
        recordAudit(administratorId(authentication), "SERVICE_PERIOD", period.id, "CREATED", null,
                period.name + ":" + period.dayOfWeek, null);
        return servicePeriodView(period);
    }

    @PutMapping("/settings/service-periods/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ServicePeriodView updateServicePeriod(@PathVariable UUID id,
            @Valid @RequestBody ServicePeriodUpdate input, Authentication authentication) {
        RestaurantServicePeriod period = servicePeriods.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service period", id.toString()));
        validateServicePeriod(input, id);
        ensureFutureBookingsRemainInService(period, input);
        String old = period.name + ":" + period.dayOfWeek + ":" + period.startTime + "-" + period.endTime + ":" + period.active;
        applyServicePeriod(period, input);
        servicePeriods.save(period);
        recordAudit(administratorId(authentication), "SERVICE_PERIOD", id, "UPDATED", old,
                period.name + ":" + period.dayOfWeek + ":" + period.startTime + "-" + period.endTime + ":" + period.active, null);
        return servicePeriodView(period);
    }

    @DeleteMapping("/settings/service-periods/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deactivateServicePeriod(@PathVariable UUID id, Authentication authentication) {
        RestaurantServicePeriod period = servicePeriods.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service period", id.toString()));
        ServicePeriodUpdate inactive = new ServicePeriodUpdate(period.name, period.dayOfWeek, period.startTime,
                period.endTime, period.slotIntervalMinutes, period.diningDurationMinutes, false);
        ensureFutureBookingsRemainInService(period, inactive);
        period.active = false;
        servicePeriods.save(period);
        recordAudit(administratorId(authentication), "SERVICE_PERIOD", id, "DEACTIVATED",
                period.name, "inactive", null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings/blocked-dates")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional(readOnly = true)
    public List<BlockedDateView> blockedDateList(
            @RequestParam(defaultValue = "180") @Min(1) @Max(365) int days) {
        LocalDate from = LocalDate.now(currentZone());
        return blockedDates.findByBlockedDateBetween(from, from.plusDays(days), Sort.by("blockedDate"))
                .stream().map(this::blockedDateView).toList();
    }

    @PostMapping("/settings/blocked-dates")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public BlockedDateView createBlockedDate(@Valid @RequestBody BlockedDateUpdate input,
                                              Authentication authentication) {
        validateBlockedDate(input);
        boolean overlapsBooking = reservations.findByReservationDateAndStatusIn(
                input.blockedDate(), ACTIVE_STATUSES).stream()
                .anyMatch(reservation -> input.fullDay()
                        || intervalsOverlap(input.startTime(), input.endTime(),
                                reservation.startTime, reservation.endTime));
        if (overlapsBooking)
            throw new BusinessRuleException("BLOCKED_DATE_HAS_RESERVATIONS",
                    "This closure conflicts with an existing reservation.", HttpStatus.CONFLICT);
        boolean overlapsClosure = blockedDates.findByBlockedDate(input.blockedDate()).stream()
                .anyMatch(block -> input.fullDay() || block.fullDay
                        || intervalsOverlap(input.startTime(), input.endTime(), block.startTime, block.endTime));
        if (overlapsClosure)
            throw new BusinessRuleException("BLOCKED_DATE_OVERLAP",
                    "This closure overlaps an existing closure.", HttpStatus.CONFLICT);
        RestaurantBlockedDate block = new RestaurantBlockedDate();
        block.id = UUID.randomUUID();
        block.blockedDate = input.blockedDate();
        block.reason = input.reason().trim();
        block.fullDay = input.fullDay();
        block.startTime = input.fullDay() ? null : input.startTime();
        block.endTime = input.fullDay() ? null : input.endTime();
        block.createdAt = LocalDateTime.now();
        blockedDates.save(block);
        recordAudit(administratorId(authentication), "BLOCKED_DATE", block.id, "CREATED", null,
                block.blockedDate + ":" + block.reason, null);
        return blockedDateView(block);
    }

    @DeleteMapping("/settings/blocked-dates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteBlockedDate(@PathVariable UUID id, Authentication authentication) {
        RestaurantBlockedDate block = blockedDates.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blocked date", id.toString()));
        blockedDates.delete(block);
        recordAudit(administratorId(authentication), "BLOCKED_DATE", id, "DELETED",
                block.blockedDate + ":" + block.reason, null, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional(readOnly = true)
    public PageView<NotificationView> listNotifications(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        Page<NotificationOutbox> result = notifications.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageView.from(result.map(value -> new NotificationView(value.id, value.notificationType,
                value.aggregateType, value.aggregateId, maskAddress(value.recipientEmail), value.status,
                value.attemptCount, value.lastAttemptAt, value.sentAt, value.providerMessageId,
                value.lastError, value.createdAt)));
    }

    private static String maskAddress(String email) {
        int at = email.indexOf('@');
        if (at < 1) return "hidden";
        return email.substring(0, 1) + "***" + email.substring(at);
    }

    private Reservation loadReservation(UUID id) {
        return reservations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation", id.toString()));
    }
    private ReservationSummary summary(Reservation r, boolean demo) {
        return new ReservationSummary(r.id, r.confirmationCode,
                demo ? "Guest" : r.guest.firstName + " " + r.guest.lastName,
                r.reservationDate, r.startTime, r.partySize, r.table.displayName,
                r.experience == null ? null : r.experience.name, r.status, r.specialOccasion != null);
    }
    private InquirySummary inquirySummary(PrivateDiningInquiry i) {
        return new InquirySummary(i.id, i.referenceNumber, i.firstName + " " + i.lastName,
                i.preferredDate, i.guestCount, i.eventType, i.status, i.createdAt);
    }
    private TableView tableView(DiningTable t) {
        return new TableView(t.id, t.tableNumber, t.displayName, t.minimumCapacity, t.maximumCapacity, t.location, t.active);
    }
    private void applyTable(DiningTable t, TableUpdate update) {
        t.tableNumber = update.tableNumber().trim();
        t.displayName = update.displayName().trim();
        t.minimumCapacity = update.minimumCapacity();
        t.maximumCapacity = update.maximumCapacity();
        t.location = update.location().trim().toUpperCase(Locale.ROOT);
        t.active = update.active();
    }
    private void validateTableCapacity(TableUpdate input) {
        if (input.minimumCapacity() > input.maximumCapacity())
            throw new BusinessRuleException("TABLE_INVALID_CAPACITY",
                    "Maximum capacity must be greater than or equal to minimum capacity.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
    }
    private SettingsView settingsView(RestaurantSettings s) {
        return new SettingsView(s.restaurantTimezone, s.minimumAdvanceBookingHours, s.maximumAdvanceBookingDays,
                s.minimumPartySize, s.maximumOnlinePartySize, s.defaultReservationDurationMinutes,
                s.reservationIntervalMinutes, s.cancellationCutoffHours, s.bookingEnabled, s.privateDiningEnabled);
    }
    private ServicePeriodView servicePeriodView(RestaurantServicePeriod p) {
        return new ServicePeriodView(p.id, p.name, p.dayOfWeek, p.startTime, p.endTime,
                p.slotIntervalMinutes, p.diningDurationMinutes, p.active);
    }
    private BlockedDateView blockedDateView(RestaurantBlockedDate b) {
        return new BlockedDateView(b.id, b.blockedDate, b.reason, b.fullDay, b.startTime, b.endTime);
    }
    private void validateServicePeriod(ServicePeriodUpdate input, UUID excludeId) {
        if (!input.endTime().isAfter(input.startTime()))
            throw new BusinessRuleException("INVALID_SERVICE_PERIOD", "End time must be after start time.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        boolean overlaps = input.active()
                && servicePeriods.findByDayOfWeekAndActiveTrueOrderByStartTimeAsc(input.dayOfWeek()).stream()
                .filter(period -> !period.id.equals(excludeId))
                .anyMatch(period -> intervalsOverlap(input.startTime(), input.endTime(), period.startTime, period.endTime));
        if (overlaps)
            throw new BusinessRuleException("SERVICE_PERIOD_OVERLAP",
                    "Active service periods on the same day cannot overlap.", HttpStatus.CONFLICT);
    }
    private void ensureFutureBookingsRemainInService(RestaurantServicePeriod current, ServicePeriodUpdate update) {
        LocalDate today = LocalDate.now(currentZone());
        boolean conflict = reservations.findByReservationDateGreaterThanEqualAndStatusIn(today, ACTIVE_STATUSES).stream()
                .filter(reservation -> reservation.reservationDate.getDayOfWeek().getValue() == current.dayOfWeek)
                .anyMatch(reservation -> current.dayOfWeek != update.dayOfWeek() || !update.active()
                        || reservation.startTime.isBefore(update.startTime())
                        || reservation.endTime.isAfter(update.endTime()));
        if (conflict)
            throw new BusinessRuleException("SERVICE_PERIOD_HAS_RESERVATIONS",
                    "This service-period change conflicts with upcoming reservations.", HttpStatus.CONFLICT);
    }
    private void applyServicePeriod(RestaurantServicePeriod period, ServicePeriodUpdate input) {
        period.name = input.name().trim();
        period.dayOfWeek = input.dayOfWeek();
        period.startTime = input.startTime();
        period.endTime = input.endTime();
        period.slotIntervalMinutes = input.slotIntervalMinutes();
        period.diningDurationMinutes = input.diningDurationMinutes();
        period.active = input.active();
    }
    private void validateBlockedDate(BlockedDateUpdate input) {
        if (input.blockedDate().isBefore(LocalDate.now(currentZone())))
            throw new BusinessRuleException("BLOCKED_DATE_IN_PAST", "Closure date must be today or later.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        if (!input.fullDay() && (input.startTime() == null || input.endTime() == null
                || !input.endTime().isAfter(input.startTime())))
            throw new BusinessRuleException("INVALID_BLOCKED_INTERVAL",
                    "Partial closures require a valid start and end time.", HttpStatus.UNPROCESSABLE_ENTITY);
    }
    private java.time.ZoneId currentZone() { return java.time.ZoneId.of(currentSettings().restaurantTimezone); }
    private static boolean intervalsOverlap(java.time.LocalTime start, java.time.LocalTime end,
                                            java.time.LocalTime otherStart, java.time.LocalTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }
    private void recordAudit(UUID adminId, String type, UUID id, String action, String oldValue, String newValue,
                             String reason) {
        AdministrativeAuditLog entry = new AdministrativeAuditLog();
        entry.id = UUID.randomUUID();
        entry.administratorId = adminId;
        entry.entityType = type;
        entry.entityId = id;
        entry.action = action;
        entry.previousValue = oldValue;
        entry.newValue = newValue;
        entry.reason = reason;
        entry.createdAt = LocalDateTime.now();
        audit.save(entry);
    }
    private UUID administratorId(Authentication authentication) {
        return administrators.findByEmailIgnoreCase(authentication.getName())
                .filter(value -> value.active)
                .map(value -> value.id)
                .orElseThrow(() -> new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                        "Authenticated administrator account is not active."));
    }
    private boolean isDemoViewer(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DEMO_VIEWER"));
    }
    private RestaurantSettings currentSettings() {
        return settings.findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant settings", "settings"));
    }
    private static boolean validTransition(String from, String to) {
        return switch (from) {
            case "CONFIRMED", "PENDING" -> Set.of("SEATED", "CANCELLED", "NO_SHOW").contains(to);
            case "SEATED" -> "COMPLETED".equals(to);
            default -> false;
        };
    }
    private static boolean validInquiryTransition(String from, String to) {
        return switch (from) {
            case "NEW" -> Set.of("CONTACTED", "DECLINED", "CANCELLED").contains(to);
            case "CONTACTED" -> Set.of("PROPOSAL_SENT", "BOOKED", "DECLINED", "CANCELLED").contains(to);
            case "PROPOSAL_SENT" -> Set.of("CONTACTED", "BOOKED", "DECLINED", "CANCELLED").contains(to);
            case "BOOKED" -> "CANCELLED".equals(to);
            default -> false;
        };
    }
    private static void validateReservationStatus(String status) {
        if (status != null && !Set.of("PENDING", "CONFIRMED", "SEATED", "COMPLETED", "CANCELLED", "NO_SHOW").contains(status))
            throw new BusinessRuleException("INVALID_STATUS", "Unknown reservation status.", HttpStatus.BAD_REQUEST);
    }
    private static void validateInquiryStatus(String status) {
        if (status != null && !Set.of("NEW", "CONTACTED", "PROPOSAL_SENT", "BOOKED", "DECLINED", "CANCELLED").contains(status))
            throw new BusinessRuleException("INVALID_STATUS", "Unknown inquiry status.", HttpStatus.BAD_REQUEST);
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String blankToEmpty(String value) { return value == null || value.isBlank() ? "" : value.trim(); }
    private static String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        static <T> PageView<T> from(Page<T> page) {
            return new PageView<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
    public record ReservationSummary(UUID id, String confirmationCode, String guestName, LocalDate date,
            java.time.LocalTime time, int partySize, String tableName, String experienceName, String status,
            boolean specialOccasion) {}
    public record GuestView(String firstName, String lastName, String email, String phone) {}
    public record StatusHistory(String oldStatus, String newStatus, String source, UUID administratorId,
            String reason, LocalDateTime changedAt) {}
    public record ReservationDetail(UUID id, String confirmationCode, GuestView guest, LocalDate date,
            java.time.LocalTime time, java.time.LocalTime endTime, int partySize, String tableName,
            String experienceName, String status, String specialOccasion, String dietaryNotes,
            String specialRequests, String internalNotes, List<StatusHistory> history) {}
    public record Dashboard(long todaysReservations, long confirmedReservations, long expectedGuests,
            long upcomingReservations, long newInquiries, long recentCancellations,
            List<ReservationSummary> todaysService, List<ReservationSummary> upcoming, List<InquirySummary> inquiries,
            List<OperationalAlert> alerts) {}
    public record OperationalAlert(String type, String title, String detail, String href) {}
    public record InquirySummary(UUID id, String referenceNumber, String guestName, LocalDate preferredDate,
            int guestCount, String eventType, String status, LocalDateTime createdAt) {}
    public record InquiryDetail(UUID id, String referenceNumber, String firstName, String lastName, String email,
            String phoneNumber, String eventType, LocalDate preferredDate, LocalDate alternateDate, int guestCount,
            String requestedSpace, BigDecimal budgetPerGuest, String dietaryRequirements, String beveragePreferences,
            String eventDescription, String specialRequests, String preferredContactMethod, String status,
            String internalNotes, LocalDateTime createdAt) {}
    public record TableView(UUID id, String tableNumber, String displayName, int minimumCapacity, int maximumCapacity,
            String location, boolean active) {}
    public record SettingsView(String restaurantTimezone, int minimumAdvanceBookingHours,
            int maximumAdvanceBookingDays, int minimumPartySize, int maximumOnlinePartySize,
            int defaultReservationDurationMinutes, int reservationIntervalMinutes, int cancellationCutoffHours,
            boolean bookingEnabled, boolean privateDiningEnabled) {}
    public record ServicePeriodView(UUID id, String name, int dayOfWeek, java.time.LocalTime startTime,
            java.time.LocalTime endTime, int slotIntervalMinutes, int diningDurationMinutes, boolean active) {}
    public record BlockedDateView(UUID id, LocalDate date, String reason, boolean fullDay,
            java.time.LocalTime startTime, java.time.LocalTime endTime) {}
    public record NotificationView(UUID id, String type, String aggregateType, UUID aggregateId,
            String recipientHint, String status, int attempts, LocalDateTime lastAttemptAt, LocalDateTime acceptedAt,
            String providerMessageId, String errorCategory, LocalDateTime createdAt) {}
    public record StatusUpdate(@NotBlank String status, @Size(max = 500) String reason) {}
    public record AdminReservationChanges(@NotNull LocalDate reservationDate,
            @NotNull java.time.LocalTime reservationTime, @Min(1) @Max(20) int partySize,
            @Size(max = 1000) String dietaryNotes, @Size(max = 1000) String specialRequests,
            @Size(max = 500) String reason) {}
    public record TableAssignment(@NotNull UUID tableId, @Size(max = 500) String reason) {}
    public record NotesUpdate(@Size(max = 2000) String internalNotes) {}
    public record InquiryUpdate(@NotBlank String status, @Size(max = 2000) String internalNotes) {}
    public record TableUpdate(@NotBlank @Size(max = 30) String tableNumber,
            @NotBlank @Size(max = 80) String displayName, @Min(1) @Max(30) int minimumCapacity,
            @Min(1) @Max(30) int maximumCapacity, @NotBlank @Pattern(regexp = "[A-Za-z_]{2,40}") String location,
            boolean active) {}
    public record SettingsUpdate(@NotBlank String restaurantTimezone, @Min(0) @Max(720) int minimumAdvanceBookingHours,
            @Min(1) @Max(365) int maximumAdvanceBookingDays, @Min(1) @Max(20) int minimumPartySize,
            @Min(1) @Max(20) int maximumOnlinePartySize, @Min(30) @Max(600) int defaultReservationDurationMinutes,
            @Min(5) @Max(180) int reservationIntervalMinutes, @Min(0) @Max(720) int cancellationCutoffHours,
            boolean bookingEnabled, boolean privateDiningEnabled) {}
    public record ServicePeriodUpdate(@NotBlank @Size(max = 80) String name, @Min(1) @Max(7) int dayOfWeek,
            @NotNull java.time.LocalTime startTime, @NotNull java.time.LocalTime endTime,
            @Min(5) @Max(180) int slotIntervalMinutes, @Min(30) @Max(600) int diningDurationMinutes,
            boolean active) {}
    public record BlockedDateUpdate(@NotNull LocalDate blockedDate, @NotBlank @Size(max = 160) String reason,
            boolean fullDay, java.time.LocalTime startTime, java.time.LocalTime endTime) {}
}
