package com.elan.service;

import com.elan.dto.ReservationDtos.*;
import com.elan.entity.*;
import com.elan.exception.BusinessRuleException;
import com.elan.exception.ResourceNotFoundException;
import com.elan.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.elan.notification.NotificationService;

@Service
public class ReservationService {
    private static final Set<String> BLOCKING_STATUSES = Set.of("PENDING", "CONFIRMED", "SEATED");
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final DiningTableRepository tables;
    private final RestaurantServicePeriodRepository periods;
    private final RestaurantBlockedDateRepository blockedDates;
    private final ReservationRepository reservations;
    private final ReservationGuestRepository guests;
    private final ReservationStatusHistoryRepository history;
    private final ReservationIdempotencyRecordRepository idempotencyRecords;
    private final RestaurantSettingsRepository settings;
    private final DiningExperienceRepository experiences;
    private final ReservationManagementTokenRepository managementTokens;
    private final NotificationService notifications;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public ReservationService(DiningTableRepository tables, RestaurantServicePeriodRepository periods,
                              RestaurantBlockedDateRepository blockedDates, ReservationRepository reservations,
                              ReservationGuestRepository guests, ReservationStatusHistoryRepository history,
                              ReservationIdempotencyRecordRepository idempotencyRecords,
                              RestaurantSettingsRepository settings, DiningExperienceRepository experiences,
                              ReservationManagementTokenRepository managementTokens,
                              NotificationService notifications) {
        this.tables = tables;
        this.periods = periods;
        this.blockedDates = blockedDates;
        this.reservations = reservations;
        this.guests = guests;
        this.history = history;
        this.idempotencyRecords = idempotencyRecords;
        this.settings = settings;
        this.experiences = experiences;
        this.managementTokens = managementTokens;
        this.notifications = notifications;
    }

    public ReservationService(DiningTableRepository tables, RestaurantServicePeriodRepository periods,
                              RestaurantBlockedDateRepository blockedDates, ReservationRepository reservations,
                              ReservationGuestRepository guests, ReservationStatusHistoryRepository history,
                              ReservationIdempotencyRecordRepository idempotencyRecords,
                              RestaurantSettingsRepository settings, DiningExperienceRepository experiences) {
        this(tables, periods, blockedDates, reservations, guests, history, idempotencyRecords, settings,
                experiences, null, null);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(LocalDate date, int partySize, UUID experienceId) {
        RestaurantSettings config = configuration();
        ZoneId zone = ZoneId.of(config.restaurantTimezone);
        validatePartySize(config, partySize);
        List<AvailabilitySlot> slots = new ArrayList<>();
        if (!withinBookingWindow(config, date, zone) || !config.bookingEnabled) {
            return new AvailabilityResponse(date, partySize, zone.getId(), slots);
        }
        DiningExperience experience = loadExperience(experienceId, date);
        List<DiningTable> activeTables = tables.findByActiveTrueOrderByMaximumCapacityAscTableNumberAsc();
        for (RestaurantServicePeriod period : servicePeriods(date)) {
            int duration = duration(period, config, experience);
            for (LocalTime time = period.startTime; !time.plusMinutes(duration).isAfter(period.endTime);
                 time = time.plusMinutes(period.slotIntervalMinutes)) {
                boolean available = !blocked(date, time, time.plusMinutes(duration))
                        && isBookableAt(date, time, config, zone)
                        && selectTable(activeTables, date, time, time.plusMinutes(duration), partySize,
                        experience == null ? null : experience.id, null) != null;
                slots.add(new AvailabilitySlot(time, available));
            }
        }
        return new AvailabilityResponse(date, partySize, zone.getId(), slots);
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request, String idempotencyKey) {
        return create(request, idempotencyKey, "CUSTOMER", null);
    }

    @Transactional
    public ReservationResponse createForAdministrator(ReservationRequest request, String idempotencyKey,
                                                       UUID administratorId) {
        return create(request, idempotencyKey, "ADMINISTRATOR", administratorId);
    }

    private ReservationResponse create(ReservationRequest request, String idempotencyKey,
                                       String source, UUID administratorId) {
        RestaurantSettings config = configuration();
        ZoneId zone = ZoneId.of(config.restaurantTimezone);
        if (idempotencyKey == null || idempotencyKey.length() < 16 || idempotencyKey.length() > 128) {
            throw new BusinessRuleException("IDEMPOTENCY_KEY_INVALID",
                    "A valid idempotency key is required.", HttpStatus.BAD_REQUEST);
        }
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime idempotencyNow = LocalDateTime.now(ZoneOffset.UTC);
        idempotencyRecords.deleteByExpiresAtBefore(idempotencyNow);
        if (managementTokens != null) managementTokens.deleteByExpiresAtBefore(idempotencyNow);
        String keyHash = hashToken(idempotencyKey);
        String requestHash = hashToken(request.toString());
        ReservationResponse replay = replayIfPresent(keyHash, requestHash, idempotencyNow);
        if (replay != null) return replay;
        validatePartySize(config, request.partySize());
        requireBookable(config, request.reservationDate(), request.reservationTime(), zone);
        DiningExperience experience = loadExperience(request.experienceId(), request.reservationDate());
        RestaurantServicePeriod period = findPeriod(request.reservationDate(), request.reservationTime());
        int duration = duration(period, config, experience);
        LocalTime end = request.reservationTime().plusMinutes(duration);
        if (end.isAfter(period.endTime) || blocked(request.reservationDate(), request.reservationTime(), end)) {
            throw unavailable();
        }

        List<DiningTable> lockedTables = tables.lockAllActiveTables();
        replay = replayIfPresent(keyHash, requestHash, LocalDateTime.now(ZoneOffset.UTC));
        if (replay != null) return replay;
        DiningTable selected = selectTable(lockedTables, request.reservationDate(), request.reservationTime(),
                end, request.partySize(), experience == null ? null : experience.id, null);
        if (selected == null) throw unavailable();

        ReservationGuest guest = guests
                .findFirstByEmailIgnoreCaseAndFirstNameIgnoreCaseAndLastNameIgnoreCase(
                        request.email().trim(), request.firstName().trim(), request.lastName().trim())
                .orElseGet(() -> newGuest(request, now));
        guest.phoneNumber = request.phoneNumber().trim();
        guest.updatedAt = now;
        guests.save(guest);

        String token = newManagementToken();
        Reservation reservation = new Reservation();
        reservation.id = UUID.randomUUID();
        reservation.confirmationCode = newConfirmationCode();
        reservation.guest = guest;
        reservation.table = selected;
        reservation.experience = experience;
        reservation.partySize = request.partySize();
        reservation.reservationDate = request.reservationDate();
        reservation.startTime = request.reservationTime();
        reservation.endTime = end;
        reservation.status = "CONFIRMED";
        reservation.specialOccasion = request.specialOccasion();
        reservation.dietaryNotes = trimToNull(request.dietaryNotes());
        reservation.specialRequests = trimToNull(request.specialRequests());
        reservation.managementTokenHash = hashToken(token);
        reservation.managementTokenExpiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(180);
        reservation.createdAt = now;
        reservation.updatedAt = now;
        reservations.save(reservation);
        storeManagementToken(reservation, token, LocalDateTime.now(ZoneOffset.UTC).plusDays(180));
        recordStatus(reservation, null, "CONFIRMED", source, null, now, administratorId);
        ReservationIdempotencyRecord record = idempotencyRecords.lockByKeyHash(keyHash).orElseGet(() -> {
            ReservationIdempotencyRecord created = new ReservationIdempotencyRecord();
            created.id = UUID.randomUUID();
            created.keyHash = keyHash;
            return created;
        });
        record.requestHash = requestHash;
        record.reservation = reservation;
        record.createdAt = idempotencyNow;
        record.expiresAt = idempotencyNow.plusHours(24);
        idempotencyRecords.save(record);
        if (notifications != null) notifications.reservation(reservation, "RESERVATION_CONFIRMATION", token);
        return response(reservation, token);
    }

    private ReservationResponse replayIfPresent(String keyHash, String requestHash, LocalDateTime now) {
        ReservationIdempotencyRecord record = idempotencyRecords.lockByKeyHash(keyHash).orElse(null);
        if (record == null || !record.expiresAt.isAfter(now)) return null;
        if (!record.requestHash.equals(requestHash)) {
            throw new BusinessRuleException("IDEMPOTENCY_KEY_REUSED",
                    "This idempotency key was already used for a different request.", HttpStatus.CONFLICT);
        }
        String rotatedToken = newManagementToken();
        record.reservation.managementTokenHash = hashToken(rotatedToken);
        record.reservation.managementTokenExpiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(180);
        record.reservation.updatedAt = now;
        reservations.save(record.reservation);
        storeManagementToken(record.reservation, rotatedToken, record.reservation.managementTokenExpiresAt);
        return response(record.reservation, rotatedToken);
    }

    @Transactional(readOnly = true)
    public ManagedReservation manage(String token) {
        Reservation reservation = authorizedReservation(token);
        RestaurantSettings config = configuration();
        return new ManagedReservation(reservation.confirmationCode, reservation.status, reservation.reservationDate,
                reservation.startTime, reservation.partySize, reservation.table.displayName,
                reservation.experience == null ? null : reservation.experience.name, reservation.specialOccasion,
                reservation.dietaryNotes, reservation.specialRequests,
                "Changes or cancellations must be made at least " + config.cancellationCutoffHours + " hours before arrival.");
    }

    @Transactional
    public ManagedReservation modify(String token, ReservationChanges changes) {
        Reservation reservation = authorizedReservation(token);
        return modifyReservation(reservation, changes);
    }

    @Transactional
    public ManagedReservation modifyByAdministrator(UUID reservationId, ReservationChanges changes) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId.toString()));
        return modifyReservation(reservation, changes);
    }

    private ManagedReservation modifyReservation(Reservation reservation, ReservationChanges changes) {
        ensureCanChange(reservation);
        List<DiningTable> lockedTables = tables.lockAllActiveTables();
        RestaurantSettings config = configuration();
        ZoneId zone = ZoneId.of(config.restaurantTimezone);
        validatePartySize(config, changes.partySize());
        requireBookable(config, changes.reservationDate(), changes.reservationTime(), zone);
        RestaurantServicePeriod period = findPeriod(changes.reservationDate(), changes.reservationTime());
        int diningDuration = duration(period, config, reservation.experience);
        LocalTime end = changes.reservationTime().plusMinutes(diningDuration);
        if (end.isAfter(period.endTime) || blocked(changes.reservationDate(), changes.reservationTime(), end)) {
            throw unavailable();
        }
        DiningTable table = selectTable(lockedTables, changes.reservationDate(), changes.reservationTime(),
                end, changes.partySize(), reservation.experience == null ? null : reservation.experience.id,
                reservation.id);
        if (table == null) throw unavailable();

        reservation.table = table;
        reservation.partySize = changes.partySize();
        reservation.reservationDate = changes.reservationDate();
        reservation.startTime = changes.reservationTime();
        reservation.endTime = end;
        reservation.dietaryNotes = trimToNull(changes.dietaryNotes());
        reservation.specialRequests = trimToNull(changes.specialRequests());
        reservation.updatedAt = LocalDateTime.now(zone);
        reservations.save(reservation);
        if (notifications != null) notifications.reservation(reservation, "RESERVATION_MODIFICATION", null);
        return toManaged(reservation, config);
    }

    @Transactional
    public ManagedReservation reassignTableByAdministrator(UUID reservationId, UUID tableId) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId.toString()));
        ensureCanChange(reservation);
        List<DiningTable> activeTables = tables.lockAllActiveTables();
        DiningTable table = activeTables.stream().filter(candidate -> candidate.id.equals(tableId)).findFirst()
                .orElseThrow(() -> new BusinessRuleException("TABLE_NOT_AVAILABLE",
                        "Choose an active table.", HttpStatus.UNPROCESSABLE_ENTITY));
        if (table.minimumCapacity > reservation.partySize || table.maximumCapacity < reservation.partySize)
            throw new BusinessRuleException("TABLE_CAPACITY_MISMATCH",
                    "The selected table cannot accommodate this party size.", HttpStatus.UNPROCESSABLE_ENTITY);
        if (reservation.experience != null) {
            List<UUID> restricted = tables.findRestrictedTableIds(reservation.experience.id);
            if (!restricted.isEmpty() && !restricted.contains(table.id))
                throw new BusinessRuleException("TABLE_EXPERIENCE_RESTRICTION",
                        "The selected experience is restricted to different tables.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        boolean occupied = reservations.findByTable_IdAndReservationDateAndStatusIn(
                        table.id, reservation.reservationDate, BLOCKING_STATUSES).stream()
                .filter(other -> !other.id.equals(reservation.id))
                .anyMatch(other -> other.startTime.isBefore(reservation.endTime)
                        && reservation.startTime.isBefore(other.endTime));
        if (occupied)
            throw new BusinessRuleException("TABLE_SLOT_UNAVAILABLE",
                    "The selected table is already assigned during this interval.", HttpStatus.CONFLICT);
        reservation.table = table;
        reservation.updatedAt = LocalDateTime.now(ZoneId.of(configuration().restaurantTimezone));
        reservations.save(reservation);
        return toManaged(reservation, configuration());
    }

    @Transactional
    public void cancel(String token) {
        Reservation reservation = authorizedReservation(token);
        ensureCanChange(reservation);
        RestaurantSettings config = configuration();
        ZoneId zone = ZoneId.of(config.restaurantTimezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime arrival = reservation.reservationDate.atTime(reservation.startTime).atZone(zone);
        if (!now.plusHours(config.cancellationCutoffHours).isBefore(arrival)) {
            throw new BusinessRuleException("RESERVATION_CANCELLATION_WINDOW_EXPIRED",
                    "The cancellation window for this reservation has passed.", HttpStatus.CONFLICT);
        }
        String oldStatus = reservation.status;
        LocalDateTime timestamp = LocalDateTime.now(zone);
        reservation.status = "CANCELLED";
        reservation.cancelledAt = timestamp;
        reservation.cancellationReason = "Cancelled by guest";
        reservation.updatedAt = timestamp;
        reservations.save(reservation);
        recordStatus(reservation, oldStatus, "CANCELLED", "CUSTOMER", reservation.cancellationReason, timestamp);
        if (notifications != null) notifications.reservation(reservation, "RESERVATION_CANCELLATION", null);
    }

    private ManagedReservation toManaged(Reservation reservation, RestaurantSettings config) {
        return new ManagedReservation(reservation.confirmationCode, reservation.status, reservation.reservationDate,
                reservation.startTime, reservation.partySize, reservation.table.displayName,
                reservation.experience == null ? null : reservation.experience.name, reservation.specialOccasion,
                reservation.dietaryNotes, reservation.specialRequests,
                "Changes or cancellations must be made at least " + config.cancellationCutoffHours + " hours before arrival.");
    }

    private Reservation authorizedReservation(String token) {
        if (token == null || token.length() < 40 || token.length() > 64) {
            throw invalidManagementToken();
        }
        String hash = hashToken(token);
        if (managementTokens != null) {
            Optional<ReservationManagementToken> savedToken =
                    managementTokens.findByTokenHashAndExpiresAtAfter(hash, LocalDateTime.now(ZoneOffset.UTC));
            if (savedToken.isPresent()) return savedToken.get().reservation;
        }
        return reservations.findByManagementTokenHash(hash)
                .filter(value -> value.managementTokenExpiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC)))
                .orElseThrow(this::invalidManagementToken);
    }

    private void storeManagementToken(Reservation reservation, String token, LocalDateTime expiresAt) {
        if (managementTokens == null) return;
        ReservationManagementToken entry = new ReservationManagementToken();
        entry.id = UUID.randomUUID();
        entry.reservation = reservation;
        entry.tokenHash = hashToken(token);
        entry.expiresAt = expiresAt;
        entry.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        managementTokens.save(entry);
    }

    private void ensureCanChange(Reservation reservation) {
        if (!Set.of("CONFIRMED", "PENDING").contains(reservation.status)) {
            throw new BusinessRuleException("RESERVATION_INVALID_STATE_TRANSITION",
                    "This reservation can no longer be changed.", HttpStatus.CONFLICT);
        }
    }

    private ReservationGuest newGuest(ReservationRequest request, LocalDateTime now) {
        ReservationGuest guest = new ReservationGuest();
        guest.id = UUID.randomUUID();
        guest.firstName = request.firstName().trim();
        guest.lastName = request.lastName().trim();
        guest.email = request.email().trim().toLowerCase(Locale.ROOT);
        guest.phoneNumber = request.phoneNumber().trim();
        guest.createdAt = now;
        guest.updatedAt = now;
        return guest;
    }

    private RestaurantSettings configuration() {
        return settings.findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant information", "settings"));
    }

    private void validatePartySize(RestaurantSettings config, int partySize) {
        if (partySize < config.minimumPartySize || partySize > config.maximumOnlinePartySize) {
            throw new BusinessRuleException("RESERVATION_INVALID_PARTY_SIZE",
                    "Choose a party size between " + config.minimumPartySize + " and "
                            + config.maximumOnlinePartySize + ".", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private boolean withinBookingWindow(RestaurantSettings config, LocalDate date, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        return date != null && !date.isBefore(today) && !date.isAfter(today.plusDays(config.maximumAdvanceBookingDays));
    }

    private void requireBookable(RestaurantSettings config, LocalDate date, LocalTime time, ZoneId zone) {
        if (!config.bookingEnabled) {
            throw new BusinessRuleException("BOOKING_DISABLED", "Online reservations are not currently available.",
                    HttpStatus.CONFLICT);
        }
        if (!withinBookingWindow(config, date, zone)) {
            throw new BusinessRuleException("RESERVATION_OUTSIDE_BOOKING_WINDOW",
                    "Choose a date within the restaurant's advance booking window.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!isBookableAt(date, time, config, zone)) {
            throw new BusinessRuleException("RESERVATION_OUTSIDE_BOOKING_WINDOW",
                    "Choose a time that meets the restaurant's booking policy.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private boolean isBookableAt(LocalDate date, LocalTime time, RestaurantSettings config, ZoneId zone) {
        return !date.atTime(time).atZone(zone).isBefore(ZonedDateTime.now(zone).plusHours(config.minimumAdvanceBookingHours));
    }

    private List<RestaurantServicePeriod> servicePeriods(LocalDate date) {
        return periods.findByDayOfWeekAndActiveTrueOrderByStartTimeAsc(date.getDayOfWeek().getValue());
    }

    private RestaurantServicePeriod findPeriod(LocalDate date, LocalTime time) {
        return servicePeriods(date).stream()
                .filter(period -> !time.isBefore(period.startTime) && time.isBefore(period.endTime))
                .filter(period -> Duration.between(period.startTime, time).toMinutes() % period.slotIntervalMinutes == 0)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("RESERVATION_OUTSIDE_SERVICE_HOURS",
                        "Choose one of the restaurant's available service times.", HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private DiningExperience loadExperience(UUID id, LocalDate date) {
        if (id == null) return null;
        DiningExperience experience = experiences.findById(id)
                .orElseThrow(() -> new BusinessRuleException("EXPERIENCE_NOT_BOOKABLE",
                        "The selected dining experience is not available.", HttpStatus.UNPROCESSABLE_ENTITY));
        if (!"PUBLISHED".equals(experience.status) || date.isBefore(experience.startDate) || date.isAfter(experience.endDate)) {
            throw new BusinessRuleException("EXPERIENCE_NOT_BOOKABLE",
                    "The selected dining experience is not available for this date.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return experience;
    }

    private int duration(RestaurantServicePeriod period, RestaurantSettings config, DiningExperience experience) {
        if (experience != null) return experience.reservationDurationMinutes;
        return period == null ? config.defaultReservationDurationMinutes : period.diningDurationMinutes;
    }

    private boolean blocked(LocalDate date, LocalTime start, LocalTime end) {
        return blockedDates.findByBlockedDate(date).stream().anyMatch(block -> block.fullDay
                || block.startTime.isBefore(end) && start.isBefore(block.endTime));
    }

    private DiningTable selectTable(List<DiningTable> candidates, LocalDate date, LocalTime start,
                                    LocalTime end, int partySize) {
        return selectTable(candidates, date, start, end, partySize, null, null);
    }

    private DiningTable selectTable(List<DiningTable> candidates, LocalDate date, LocalTime start,
                                    LocalTime end, int partySize, UUID experienceId, UUID ignoreReservationId) {
        Set<UUID> restrictedTables = experienceId == null ? Set.of()
                : new HashSet<>(tables.findRestrictedTableIds(experienceId));
        for (DiningTable table : candidates.stream()
                .filter(t -> t.active && t.minimumCapacity <= partySize && t.maximumCapacity >= partySize
                        && (restrictedTables.isEmpty() || restrictedTables.contains(t.id)))
                .sorted(Comparator.comparingInt((DiningTable t) -> t.maximumCapacity)
                        .thenComparing(t -> t.tableNumber)).toList()) {
            boolean occupied = reservations.findByTable_IdAndReservationDateAndStatusIn(
                            table.id, date, BLOCKING_STATUSES).stream()
                    .filter(reservation -> !reservation.id.equals(ignoreReservationId))
                    .anyMatch(reservation -> reservation.startTime.isBefore(end) && start.isBefore(reservation.endTime));
            if (!occupied) return table;
        }
        return null;
    }

    private ReservationResponse response(Reservation reservation, String token) {
        return new ReservationResponse(reservation.confirmationCode, reservation.status, reservation.reservationDate,
                reservation.startTime, reservation.endTime, reservation.partySize, reservation.table.displayName,
                reservation.experience == null ? null : reservation.experience.name, token);
    }

    private void recordStatus(Reservation reservation, String oldStatus, String newStatus, String source,
                              String reason, LocalDateTime now) {
        recordStatus(reservation, oldStatus, newStatus, source, reason, now, null);
    }

    private void recordStatus(Reservation reservation, String oldStatus, String newStatus, String source,
                              String reason, LocalDateTime now, UUID administratorId) {
        ReservationStatusHistory entry = new ReservationStatusHistory();
        entry.id = UUID.randomUUID();
        entry.reservation = reservation;
        entry.oldStatus = oldStatus;
        entry.newStatus = newStatus;
        entry.changeSource = source;
        entry.administratorId = administratorId;
        entry.reason = reason;
        entry.changedAt = now;
        history.save(entry);
    }

    private String newConfirmationCode() {
        return "ELN-" + randomCode(8);
    }

    private String newManagementToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String randomCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        return code.toString();
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private BusinessRuleException unavailable() {
        return new BusinessRuleException("RESERVATION_SLOT_UNAVAILABLE",
                "This time is no longer available. Please select another time.", HttpStatus.CONFLICT);
    }

    private BusinessRuleException invalidManagementToken() {
        return new BusinessRuleException("RESERVATION_MANAGEMENT_TOKEN_INVALID",
                "This reservation link is invalid or has expired.", HttpStatus.NOT_FOUND);
    }
}
