package com.elan.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.elan.entity.*;
import com.elan.repository.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.elan.exception.BusinessRuleException;

class ReservationServiceTest {
    private DiningTableRepository tables;
    private RestaurantServicePeriodRepository periods;
    private RestaurantBlockedDateRepository blockedDates;
    private ReservationRepository reservations;
    private ReservationGuestRepository guests;
    private ReservationStatusHistoryRepository history;
    private ReservationIdempotencyRecordRepository idempotencyRecords;
    private RestaurantSettingsRepository settings;
    private DiningExperienceRepository experiences;
    private ReservationService service;
    private LocalDate date;
    private DiningTable table;
    private RestaurantSettings config;

    @BeforeEach
    void setUp() {
        tables = mock(DiningTableRepository.class);
        periods = mock(RestaurantServicePeriodRepository.class);
        blockedDates = mock(RestaurantBlockedDateRepository.class);
        reservations = mock(ReservationRepository.class);
        guests = mock(ReservationGuestRepository.class);
        history = mock(ReservationStatusHistoryRepository.class);
        idempotencyRecords = mock(ReservationIdempotencyRecordRepository.class);
        settings = mock(RestaurantSettingsRepository.class);
        experiences = mock(DiningExperienceRepository.class);
        service = new ReservationService(tables, periods, blockedDates, reservations, guests, history,
                idempotencyRecords, settings, experiences);

        date = LocalDate.now(ZoneId.of("UTC")).plusDays(10);
        config = new RestaurantSettings();
        config.restaurantTimezone = "UTC";
        config.maximumAdvanceBookingDays = 90;
        config.minimumAdvanceBookingHours = 0;
        config.minimumPartySize = 1;
        config.maximumOnlinePartySize = 8;
        config.defaultReservationDurationMinutes = 120;
        config.bookingEnabled = true;
        when(settings.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(config));

        RestaurantServicePeriod period = new RestaurantServicePeriod();
        period.dayOfWeek = date.getDayOfWeek().getValue();
        period.startTime = LocalTime.of(17, 30);
        period.endTime = LocalTime.of(22, 0);
        period.slotIntervalMinutes = 30;
        period.diningDurationMinutes = 60;
        period.active = true;
        when(periods.findByDayOfWeekAndActiveTrueOrderByStartTimeAsc(period.dayOfWeek)).thenReturn(List.of(period));

        table = new DiningTable();
        table.id = UUID.randomUUID();
        table.tableNumber = "01";
        table.displayName = "Table 01";
        table.minimumCapacity = 1;
        table.maximumCapacity = 2;
        table.active = true;
        when(tables.findByActiveTrueOrderByMaximumCapacityAscTableNumberAsc()).thenReturn(List.of(table));
        when(blockedDates.findByBlockedDate(date)).thenReturn(List.of());
        when(reservations.findByTable_IdAndReservationDateAndStatusIn(eq(table.id), eq(date), anySet()))
                .thenReturn(List.of());
    }

    @Test
    void makesAFreeServiceTimeAvailable() {
        var result = service.availability(date, 2, null);

        assertTrue(result.availableSlots().stream()
                .anyMatch(slot -> slot.time().equals(LocalTime.of(17, 30)) && slot.available()));
        assertEquals("UTC", result.timezone());
    }

    @Test
    void excludesOverlappingReservationsButAllowsAdjacentIntervals() {
        Reservation existing = new Reservation();
        existing.id = UUID.randomUUID();
        existing.startTime = LocalTime.of(17, 30);
        existing.endTime = LocalTime.of(18, 30);
        existing.status = "CONFIRMED";
        when(reservations.findByTable_IdAndReservationDateAndStatusIn(eq(table.id), eq(date), anySet()))
                .thenReturn(List.of(existing));

        var result = service.availability(date, 2, null);

        assertFalse(result.availableSlots().stream()
                .filter(slot -> slot.time().equals(LocalTime.of(18, 0)))
                .findFirst().orElseThrow().available());
        assertTrue(result.availableSlots().stream()
                .filter(slot -> slot.time().equals(LocalTime.of(18, 30)))
                .findFirst().orElseThrow().available());
    }

    @Test
    void excludesInactiveTablesAndUnsupportedPartySizes() {
        table.active = false;
        assertFalse(service.availability(date, 2, null).availableSlots().stream().anyMatch(slot -> slot.available()));

        table.active = true;
        assertFalse(service.availability(date, 3, null).availableSlots().stream().anyMatch(slot -> slot.available()));
    }

    @Test
    void replaysAnIdempotentRequestWithoutAllocatingAnotherTable() throws Exception {
        var request = new com.elan.dto.ReservationDtos.ReservationRequest("Jordan", "Morgan",
                "jordan@example.test", "+1 555 010 0100", date, LocalTime.of(17, 30), 2,
                null, null, null, null);
        Reservation reservation = new Reservation();
        reservation.id = UUID.randomUUID();
        reservation.confirmationCode = "ELN-ABCD2345";
        reservation.status = "CONFIRMED";
        reservation.reservationDate = date;
        reservation.startTime = LocalTime.of(17, 30);
        reservation.endTime = LocalTime.of(19, 30);
        reservation.partySize = 2;
        reservation.table = table;
        reservation.managementTokenHash = "old-token-hash";

        ReservationIdempotencyRecord record = new ReservationIdempotencyRecord();
        record.id = UUID.randomUUID();
        record.keyHash = sha256("a-stable-test-idempotency-key");
        record.requestHash = sha256(request.toString());
        record.reservation = reservation;
        record.expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);
        when(idempotencyRecords.lockByKeyHash(record.keyHash)).thenReturn(Optional.of(record));

        var response = service.create(request, "a-stable-test-idempotency-key");

        assertEquals("ELN-ABCD2345", response.confirmationCode());
        assertNotNull(response.managementToken());
        assertNotEquals("old-token-hash", reservation.managementTokenHash);
        verify(tables, never()).lockAllActiveTables();
        verify(reservations).save(reservation);
        verify(guests, never()).save(any());
    }

    @Test
    void rejectsReusingAnIdempotencyKeyForDifferentRequestData() throws Exception {
        var original = new com.elan.dto.ReservationDtos.ReservationRequest("Jordan", "Morgan",
                "jordan@example.test", "+1 555 010 0100", date, LocalTime.of(17, 30), 2,
                null, null, null, null);
        var changed = new com.elan.dto.ReservationDtos.ReservationRequest("Jordan", "Morgan",
                "jordan@example.test", "+1 555 010 0100", date, LocalTime.of(18, 0), 2,
                null, null, null, null);
        ReservationIdempotencyRecord record = new ReservationIdempotencyRecord();
        record.id = UUID.randomUUID();
        record.keyHash = sha256("a-stable-test-idempotency-key");
        record.requestHash = sha256(original.toString());
        record.reservation = new Reservation();
        record.expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);
        when(idempotencyRecords.lockByKeyHash(record.keyHash)).thenReturn(Optional.of(record));

        assertThrows(BusinessRuleException.class,
                () -> service.create(changed, "a-stable-test-idempotency-key"));
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
