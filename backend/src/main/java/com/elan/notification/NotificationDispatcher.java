package com.elan.notification;

import com.elan.entity.NotificationOutbox;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@EnableScheduling
public class NotificationDispatcher {
    private static final String CLAIM_SQL = """
            UPDATE notification_outbox n
            SET status = 'PROCESSING', attempt_count = n.attempt_count + 1,
                last_attempt_at = CURRENT_TIMESTAMP,
                lease_until = CURRENT_TIMESTAMP + INTERVAL '2 minutes',
                updated_at = CURRENT_TIMESTAMP
            WHERE n.id IN (
                SELECT id FROM notification_outbox
                WHERE (status = 'PENDING' AND next_attempt_at <= CURRENT_TIMESTAMP)
                   OR (status = 'PROCESSING' AND lease_until < CURRENT_TIMESTAMP)
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT 20
            )
            RETURNING n.*
            """;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final EmailProvider provider;
    private final EmailTemplateRenderer renderer;
    private final SensitiveDataCipher cipher;

    public NotificationDispatcher(JdbcTemplate jdbc, TransactionTemplate transactions, EmailProvider provider,
            EmailTemplateRenderer renderer, SensitiveDataCipher cipher) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.provider = provider;
        this.renderer = renderer;
        this.cipher = cipher;
    }

    @Scheduled(fixedDelayString = "${elan.email.dispatch-delay-ms:5000}")
    public void dispatchPending() {
        List<NotificationOutbox> claimed = transactions.execute(status ->
                jdbc.query(CLAIM_SQL, new OutboxMapper()));
        if (claimed == null) return;
        claimed.forEach(this::deliver);
    }

    private void deliver(NotificationOutbox event) {
        try {
            String actionUrl = cipher.decrypt(event.sensitiveData).orElse(null);
            EmailProvider.DeliveryResult result = provider.send(renderer.render(event, actionUrl),
                    event.providerIdempotencyKey);
            LocalDateTime now = LocalDateTime.now();
            jdbc.update("""
                    UPDATE notification_outbox
                    SET status = ?, provider_message_id = ?, sent_at = ?, lease_until = NULL,
                        last_error = NULL, updated_at = ?
                    WHERE id = ? AND status = 'PROCESSING'
                    """, result.status(), result.providerMessageId(), now, now, event.id);
        } catch (ResendEmailProvider.DeliveryException exception) {
            fail(event, exception.category(), exception.retryable());
        } catch (RuntimeException exception) {
            fail(event, "NOTIFICATION_PROCESSING_ERROR", true);
        }
    }

    private void fail(NotificationOutbox event, String category, boolean retryable) {
        LocalDateTime now = LocalDateTime.now();
        String status;
        LocalDateTime next = now;
        if (retryable && event.attemptCount < 5) {
            int[] delays = {1, 5, 15, 60};
            next = now.plusMinutes(delays[Math.min(event.attemptCount - 1, delays.length - 1)]);
            status = "PENDING";
        } else {
            status = event.attemptCount >= 5 ? "DEAD_LETTER" : "FAILED";
        }
        jdbc.update("""
                UPDATE notification_outbox
                SET status = ?, next_attempt_at = ?, lease_until = NULL, last_error = ?, updated_at = ?
                WHERE id = ? AND status = 'PROCESSING'
                """, status, next, category, now, event.id);
        org.slf4j.LoggerFactory.getLogger(getClass()).warn(
                "Notification delivery failed: id={}, type={}, attempt={}, category={}, status={}",
                event.id, event.notificationType, event.attemptCount, category, status);
    }

    private static final class OutboxMapper implements RowMapper<NotificationOutbox> {
        @Override
        public NotificationOutbox mapRow(ResultSet rs, int rowNum) throws SQLException {
            NotificationOutbox e = new NotificationOutbox();
            e.id = rs.getObject("id", UUID.class);
            e.notificationType = rs.getString("notification_type");
            e.aggregateType = rs.getString("aggregate_type");
            e.aggregateId = rs.getObject("aggregate_id", UUID.class);
            e.recipientEmail = rs.getString("recipient_email");
            e.subject = rs.getString("subject");
            e.templateData = rs.getString("template_data");
            e.sensitiveData = rs.getBytes("sensitive_data");
            e.status = rs.getString("status");
            e.attemptCount = rs.getInt("attempt_count");
            e.providerIdempotencyKey = rs.getString("provider_idempotency_key");
            return e;
        }
    }
}
