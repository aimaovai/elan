package com.elan.notification;

public interface EmailProvider {
    DeliveryResult send(EmailMessage message, String idempotencyKey);
    record DeliveryResult(String status, String providerMessageId) {}
}
