package com.elan.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.elan.entity.NotificationOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NotificationSecurityTest {
    @Test
    void sensitivePayloadIsAuthenticatedAndEncryptedAtRest() {
        SensitiveDataCipher cipher = new SensitiveDataCipher(keyMaterial());
        String secretUrl = "https://elan.example/reservations/manage#token=private-token";
        byte[] encrypted = cipher.encrypt(secretUrl);

        assertFalse(new String(encrypted, java.nio.charset.StandardCharsets.UTF_8).contains("private-token"));
        assertEquals(secretUrl, cipher.decrypt(encrypted).orElseThrow());
        assertThrows(IllegalStateException.class,
                () -> new SensitiveDataCipher(keyMaterial()).decrypt(encryptedWithDifferentKey()));
    }

    @Test
    void malformedEncryptionKeyFailsFast() {
        assertThrows(IllegalStateException.class, () -> new SensitiveDataCipher("not-a-valid-key"));
    }

    @Test
    void renderedHtmlEscapesGuestDataAndExplainsInquiryIsNotABooking() {
        NotificationOutbox event = new NotificationOutbox();
        event.notificationType = "PRIVATE_DINING_ACKNOWLEDGMENT";
        event.aggregateType = "PRIVATE_DINING_INQUIRY";
        event.recipientEmail = "guest@example.test";
        event.subject = "We've Received Your ÉLAN Private Dining Inquiry";
        event.templateData = """
                {"firstName":"<script>alert(1)</script>","referenceNumber":"ELN-123",
                "eventType":"celebration","date":"2030-01-01","guestCount":"12"}
                """;

        EmailMessage message = new EmailTemplateRenderer(new ObjectMapper()).render(event, null);

        assertTrue(message.html().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(message.html().contains("<script>alert(1)</script>"));
        assertTrue(message.html().contains("not a confirmed event booking"));
        assertTrue(message.text().contains("not a confirmed event booking"));
    }

    @Test
    void logModeDoesNotContactProviderAndMarksDeliveryAsSimulated() {
        ResendEmailProvider provider = new ResendEmailProvider("log", false, "", "", "ÉLAN",
                "", "", "http://localhost:3000", "", RestClient.builder());
        EmailProvider.DeliveryResult result = provider.send(
                new EmailMessage("guest@example.test", "Test", "<p>body</p>", "body"), "event-key");

        assertEquals("SIMULATED", result.status());
        assertNull(result.providerMessageId());
    }

    private static String keyMaterial() {
        return Base64.getEncoder().encodeToString(new byte[32]);
    }

    private static byte[] encryptedWithDifferentKey() {
        byte[] otherKey = new byte[32];
        otherKey[0] = 1;
        return new SensitiveDataCipher(Base64.getEncoder().encodeToString(otherKey)).encrypt("irrelevant");
    }
}
