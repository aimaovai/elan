package com.elan.notification;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

@Component
public class ResendEmailProvider implements EmailProvider {
    private final String mode;
    private final boolean enabled;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final String replyTo;
    private final String testRecipient;
    private final RestClient client;

    public ResendEmailProvider(@Value("${elan.email.delivery-mode:log}") String mode,
            @Value("${elan.email.resend-enabled:false}") boolean enabled,
            @Value("${elan.email.resend-api-key:}") String apiKey,
            @Value("${elan.email.from-email:}") String fromEmail,
            @Value("${elan.email.from-name:ÉLAN Reservations}") String fromName,
            @Value("${elan.email.reply-to:}") String replyTo,
            @Value("${elan.email.test-recipient:}") String testRecipient,
            @Value("${elan.email.public-url:http://localhost:3000}") String publicUrl,
            @Value("${elan.email.encryption-key:}") String encryptionKey, RestClient.Builder builder) {
        this.mode = mode.toLowerCase(java.util.Locale.ROOT);
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.replyTo = replyTo;
        this.testRecipient = testRecipient;
        if ("live".equals(this.mode) && enabled) {
            if (!publicUrl.startsWith("https://"))
                throw new IllegalStateException("Live email requires an HTTPS APP_PUBLIC_URL.");
            if (encryptionKey.isBlank())
                throw new IllegalStateException("Live email requires ELAN_NOTIFICATION_ENCRYPTION_KEY for reservation links.");
        }
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        org.springframework.http.client.JdkClientHttpRequestFactory factory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.client = builder.requestFactory(factory).baseUrl("https://api.resend.com").build();
    }

    @Override
    public DeliveryResult send(EmailMessage message, String idempotencyKey) {
        if ("log".equals(mode)) {
            org.slf4j.LoggerFactory.getLogger(getClass()).info(
                    "Notification simulated: type not included, recipient_domain={}, subject_length={}",
                    emailDomain(message.recipient()), message.subject().length());
            return new DeliveryResult("SIMULATED", null);
        }
        if (!enabled) throw new DeliveryException("DELIVERY_DISABLED", false);
        if (apiKey.isBlank() || fromEmail.isBlank()) throw new DeliveryException("RESEND_CONFIGURATION_MISSING", false);
        if (!"test".equals(mode) && !"live".equals(mode)) throw new DeliveryException("DELIVERY_MODE_INVALID", false);
        String recipient = message.recipient();
        if ("test".equals(mode)) {
            if (testRecipient.isBlank()) throw new DeliveryException("TEST_RECIPIENT_MISSING", false);
            recipient = testRecipient;
        }
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("from", fromName + " <" + fromEmail + ">");
            body.put("to", new String[]{recipient});
            body.put("subject", message.subject());
            body.put("html", message.html());
            body.put("text", message.text());
            if (!replyTo.isBlank()) body.put("reply_to", replyTo);
            JsonNode response = client.post().uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(JsonNode.class);
            if (response == null || !response.hasNonNull("id"))
                throw new DeliveryException("RESEND_UNEXPECTED_RESPONSE", true);
            return new DeliveryResult("SENT", response.get("id").asText());
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            String category = status == 401 || status == 403 ? "RESEND_AUTH_OR_SENDER_REJECTED"
                    : status == 429 ? "RESEND_RATE_LIMITED"
                    : status >= 500 ? "RESEND_PROVIDER_UNAVAILABLE" : "RESEND_REQUEST_REJECTED";
            throw new DeliveryException(category, status == 429 || status >= 500);
        } catch (ResourceAccessException exception) {
            throw new DeliveryException("RESEND_NETWORK_FAILURE", true);
        }
    }

    private static String emailDomain(String address) {
        int at = address.lastIndexOf('@');
        return at < 0 ? "unknown" : address.substring(at + 1);
    }

    public static class DeliveryException extends RuntimeException {
        private final String category;
        private final boolean retryable;
        public DeliveryException(String category, boolean retryable) {
            super(category);
            this.category = category;
            this.retryable = retryable;
        }
        public String category() { return category; }
        public boolean retryable() { return retryable; }
    }
}
