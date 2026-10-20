package com.elan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class ReservationDtos {
    private ReservationDtos() {}

    public record AvailabilitySlot(@JsonFormat(pattern = "HH:mm") LocalTime time, boolean available) {}
    public record AvailabilityResponse(LocalDate date, int partySize, String timezone,
                                      List<AvailabilitySlot> availableSlots) {}

    public record ReservationRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^[+()0-9 .-]{7,32}$") String phoneNumber,
            @NotNull LocalDate reservationDate,
            @NotNull LocalTime reservationTime,
            @Min(1) @Max(20) int partySize,
            UUID experienceId,
            @Pattern(regexp = "BIRTHDAY|ANNIVERSARY|PROPOSAL|OTHER") String specialOccasion,
            @Size(max = 1000) String dietaryNotes,
            @Size(max = 1000) String specialRequests) {}

    public record ReservationResponse(String confirmationCode, String status, LocalDate reservationDate,
                                      @JsonFormat(pattern = "HH:mm") LocalTime reservationTime,
                                      @JsonFormat(pattern = "HH:mm") LocalTime endTime, int partySize,
                                      String tableName, String experienceName, String managementToken) {}

    public record ManagedReservation(String confirmationCode, String status, LocalDate reservationDate,
                                     @JsonFormat(pattern = "HH:mm") LocalTime reservationTime,
                                     int partySize, String tableName,
                                     String experienceName, String specialOccasion, String dietaryNotes,
                                     String specialRequests, String cancellationPolicy) {}

    public record ReservationChanges(
            @NotNull LocalDate reservationDate,
            @NotNull LocalTime reservationTime,
            @Min(1) @Max(20) int partySize,
            @Size(max = 1000) String dietaryNotes,
            @Size(max = 1000) String specialRequests) {}

    public record PrivateDiningInquiryRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^[+()0-9 .-]{7,32}$") String phoneNumber,
            @NotBlank @Pattern(regexp = "BIRTHDAY|ANNIVERSARY|CORPORATE|WEDDING_RELATED|CELEBRATION|OTHER") String eventType,
            @NotNull LocalDate preferredDate,
            LocalDate alternateDate,
            @Min(1) @Max(200) int guestCount,
            UUID privateDiningSpaceId,
            @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) java.math.BigDecimal budgetPerGuest,
            @Size(max = 1000) String dietaryRequirements,
            @Size(max = 1000) String beveragePreferences,
            @NotBlank @Size(max = 2000) String eventDescription,
            @Size(max = 1000) String specialRequests,
            @NotBlank @Pattern(regexp = "EMAIL|PHONE") String preferredContactMethod) {}

    public record PrivateDiningInquiryResponse(String referenceNumber, String status, boolean isConfirmedEvent) {}
}
