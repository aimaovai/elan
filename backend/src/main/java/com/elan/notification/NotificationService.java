package com.elan.notification;

import com.elan.entity.DiningExperience;
import com.elan.entity.NotificationOutbox;
import com.elan.entity.PrivateDiningInquiry;
import com.elan.entity.Reservation;
import com.elan.repository.NotificationOutboxRepository;
import com.elan.repository.RestaurantSettingsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationOutboxRepository outbox;
    private final RestaurantSettingsRepository settings;
    private final ObjectMapper mapper;
    private final SensitiveDataCipher cipher;
    private final String publicUrl;
    private final String internalRecipient;

    public NotificationService(NotificationOutboxRepository outbox, RestaurantSettingsRepository settings,
            ObjectMapper mapper, SensitiveDataCipher cipher,
            @Value("${elan.email.public-url:}") String publicUrl,
            @Value("${elan.email.private-dining-admin-email:}") String internalRecipient) {
        this.outbox = outbox;
        this.settings = settings;
        this.mapper = mapper;
        this.cipher = cipher;
        this.publicUrl = publicUrl == null ? "" : publicUrl.replaceAll("/+$", "");
        this.internalRecipient = internalRecipient == null ? "" : internalRecipient.trim();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reservation(Reservation reservation, String type, String managementToken) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("firstName", reservation.guest.firstName);
        data.put("confirmationCode", reservation.confirmationCode);
        data.put("date", reservation.reservationDate.toString());
        data.put("time", reservation.startTime.toString());
        data.put("partySize", Integer.toString(reservation.partySize));
        DiningExperience experience = reservation.experience;
        data.put("experience", experience == null ? "" : experience.name);
        var restaurant = settings.findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new IllegalStateException("Restaurant settings are required for reservation email."));
        data.put("restaurantCity", restaurant.city);
        data.put("cancellationPolicy", restaurant.reservationPolicy);
        data.put("restaurantHours", restaurant.openingHours);
        String subject = switch (type) {
            case "RESERVATION_CONFIRMATION" -> "Your ÉLAN Reservation Is Confirmed";
            case "RESERVATION_MODIFICATION" -> "Your ÉLAN Reservation Has Been Updated";
            case "RESERVATION_CANCELLATION" -> "Your ÉLAN Reservation Has Been Cancelled";
            default -> throw new IllegalArgumentException("Unsupported reservation notification type.");
        };
        enqueue(type, "RESERVATION", reservation.id, reservation.guest.email, subject, data,
                "RESERVATION_CONFIRMATION".equals(type) && !publicUrl.isBlank()
                        ? cipher.encrypt(publicUrl + "/reservations/manage#token="
                                + java.net.URLEncoder.encode(managementToken, java.nio.charset.StandardCharsets.UTF_8)) : null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void inquiry(PrivateDiningInquiry inquiry) {
        Map<String, String> guestData = new LinkedHashMap<>();
        guestData.put("firstName", inquiry.firstName);
        guestData.put("referenceNumber", inquiry.referenceNumber);
        guestData.put("eventType", inquiry.eventType.replace('_', ' ').toLowerCase(java.util.Locale.ROOT));
        guestData.put("date", inquiry.preferredDate.toString());
        guestData.put("guestCount", Integer.toString(inquiry.guestCount));
        enqueue("PRIVATE_DINING_ACKNOWLEDGMENT", "PRIVATE_DINING_INQUIRY", inquiry.id, inquiry.email,
                "We've Received Your ÉLAN Private Dining Inquiry", guestData, null);
        if (!internalRecipient.isBlank()) {
            Map<String, String> adminData = new LinkedHashMap<>();
            adminData.put("referenceNumber", inquiry.referenceNumber);
            adminData.put("guestName", inquiry.firstName + " " + inquiry.lastName);
            adminData.put("eventType", inquiry.eventType);
            adminData.put("date", inquiry.preferredDate.toString());
            adminData.put("guestCount", Integer.toString(inquiry.guestCount));
            enqueue("PRIVATE_DINING_ADMIN_NOTIFICATION", "PRIVATE_DINING_INQUIRY", inquiry.id,
                    internalRecipient, "New ÉLAN Private Dining Inquiry", adminData, null);
        }
    }

    private void enqueue(String type, String aggregateType, UUID aggregateId, String recipient, String subject,
                        Map<String, String> data, byte[] sensitiveData) {
        NotificationOutbox event = new NotificationOutbox();
        event.id = UUID.randomUUID();
        event.notificationType = type;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.recipientEmail = recipient;
        event.subject = subject;
        try {
            event.templateData = mapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to prepare notification content.", exception);
        }
        event.sensitiveData = sensitiveData;
        event.status = "PENDING";
        event.attemptCount = 0;
        event.nextAttemptAt = LocalDateTime.now();
        event.providerIdempotencyKey = event.id.toString();
        event.createdAt = LocalDateTime.now();
        event.updatedAt = event.createdAt;
        outbox.save(event);
    }
}
