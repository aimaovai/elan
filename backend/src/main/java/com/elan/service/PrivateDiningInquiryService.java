package com.elan.service;

import com.elan.dto.ReservationDtos.PrivateDiningInquiryRequest;
import com.elan.dto.ReservationDtos.PrivateDiningInquiryResponse;
import com.elan.entity.PrivateDiningInquiry;
import com.elan.entity.PrivateDiningSpace;
import com.elan.entity.RestaurantSettings;
import com.elan.exception.BusinessRuleException;
import com.elan.exception.ResourceNotFoundException;
import com.elan.repository.PrivateDiningInquiryRepository;
import com.elan.repository.PrivateDiningSpaceRepository;
import com.elan.repository.RestaurantSettingsRepository;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivateDiningInquiryService {
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final PrivateDiningInquiryRepository inquiries;
    private final PrivateDiningSpaceRepository spaces;
    private final RestaurantSettingsRepository settings;
    private final SecureRandom random = new SecureRandom();

    public PrivateDiningInquiryService(PrivateDiningInquiryRepository inquiries, PrivateDiningSpaceRepository spaces,
                                       RestaurantSettingsRepository settings) {
        this.inquiries = inquiries;
        this.spaces = spaces;
        this.settings = settings;
    }

    @Transactional
    public PrivateDiningInquiryResponse create(PrivateDiningInquiryRequest request) {
        RestaurantSettings config = settings.findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant information", "settings"));
        if (!config.privateDiningEnabled) {
            throw new BusinessRuleException("PRIVATE_DINING_DISABLED",
                    "Private dining inquiries are not currently available.", HttpStatus.CONFLICT);
        }
        LocalDate today = LocalDate.now(ZoneId.of(config.restaurantTimezone));
        if (request.preferredDate().isBefore(today)
                || request.alternateDate() != null && request.alternateDate().isBefore(today)) {
            throw new BusinessRuleException("PRIVATE_DINING_INVALID_DATE",
                    "Inquiry dates must be today or in the future.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        PrivateDiningSpace space = null;
        if (request.privateDiningSpaceId() != null) {
            space = spaces.findById(request.privateDiningSpaceId())
                    .filter(value -> value.active)
                    .orElseThrow(() -> new BusinessRuleException("PRIVATE_DINING_SPACE_UNAVAILABLE",
                            "The selected private dining space is not available.", HttpStatus.UNPROCESSABLE_ENTITY));
            if (request.guestCount() > space.capacity) {
                throw new BusinessRuleException("PRIVATE_DINING_SPACE_CAPACITY_EXCEEDED",
                        "The guest count exceeds this space's listed capacity.", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of(config.restaurantTimezone));
        PrivateDiningInquiry inquiry = new PrivateDiningInquiry();
        inquiry.id = UUID.randomUUID();
        inquiry.referenceNumber = "PD-" + randomCode();
        inquiry.firstName = request.firstName().trim();
        inquiry.lastName = request.lastName().trim();
        inquiry.email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        inquiry.phoneNumber = request.phoneNumber().trim();
        inquiry.eventType = request.eventType();
        inquiry.preferredDate = request.preferredDate();
        inquiry.alternateDate = request.alternateDate();
        inquiry.guestCount = request.guestCount();
        inquiry.space = space;
        inquiry.budgetPerGuest = request.budgetPerGuest();
        inquiry.dietaryRequirements = clean(request.dietaryRequirements());
        inquiry.beveragePreferences = clean(request.beveragePreferences());
        inquiry.eventDescription = request.eventDescription().trim();
        inquiry.specialRequests = clean(request.specialRequests());
        inquiry.preferredContactMethod = request.preferredContactMethod();
        inquiry.status = "NEW";
        inquiry.createdAt = now;
        inquiry.updatedAt = now;
        inquiries.save(inquiry);
        return new PrivateDiningInquiryResponse(inquiry.referenceNumber, inquiry.status, false);
    }

    private String randomCode() {
        StringBuilder result = new StringBuilder(8);
        for (int i = 0; i < 8; i++) result.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        return result.toString();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
