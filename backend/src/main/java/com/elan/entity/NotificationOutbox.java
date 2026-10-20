package com.elan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox {
    @Id public UUID id;
    @Column(name = "notification_type", nullable = false, length = 50) public String notificationType;
    @Column(name = "aggregate_type", nullable = false, length = 40) public String aggregateType;
    @Column(name = "aggregate_id", nullable = false) public UUID aggregateId;
    @Column(name = "recipient_email", nullable = false, length = 254) public String recipientEmail;
    @Column(nullable = false, length = 200) public String subject;
    @Column(name = "template_data", nullable = false, columnDefinition = "text") public String templateData;
    @Column(name = "sensitive_data", columnDefinition = "bytea") public byte[] sensitiveData;
    @Column(nullable = false, length = 20) public String status;
    @Column(name = "attempt_count", nullable = false) public int attemptCount;
    @Column(name = "next_attempt_at", nullable = false) public LocalDateTime nextAttemptAt;
    @Column(name = "last_attempt_at") public LocalDateTime lastAttemptAt;
    @Column(name = "lease_until") public LocalDateTime leaseUntil;
    @Column(name = "sent_at") public LocalDateTime sentAt;
    @Column(name = "provider_message_id", length = 120) public String providerMessageId;
    @Column(name = "provider_idempotency_key", nullable = false, unique = true, length = 80)
    public String providerIdempotencyKey;
    @Column(name = "last_error", length = 80) public String lastError;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
}
