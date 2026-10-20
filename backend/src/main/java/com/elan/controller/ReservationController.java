package com.elan.controller;

import com.elan.dto.ReservationDtos.*;
import com.elan.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationService reservations;

    public ReservationController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @GetMapping("/availability")
    @Operation(summary = "Find bookable reservation times")
    public AvailabilityResponse availability(@RequestParam LocalDate date,
                                              @RequestParam @Min(1) @Max(20) int partySize,
                                              @RequestParam(required = false) UUID experienceId) {
        return reservations.availability(date, partySize, experienceId);
    }

    @PostMapping
    @Operation(summary = "Create a confirmed reservation after allocating a table")
    public ResponseEntity<ReservationResponse> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                        @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(reservations.create(request, idempotencyKey));
    }

    @GetMapping("/manage")
    @Operation(summary = "View a reservation using its private management token")
    public ResponseEntity<ManagedReservation> manage(
            @RequestHeader("X-Reservation-Management-Token") String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reservations.manage(token));
    }

    @PatchMapping("/manage")
    @Operation(summary = "Modify a reservation using its private management token")
    public ResponseEntity<ManagedReservation> modify(
            @RequestHeader("X-Reservation-Management-Token") String token,
            @Valid @RequestBody ReservationChanges changes) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reservations.modify(token, changes));
    }

    @DeleteMapping("/manage")
    @Operation(summary = "Cancel a reservation using its private management token")
    public ResponseEntity<Void> cancel(
            @RequestHeader("X-Reservation-Management-Token") String token) {
        reservations.cancel(token);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
