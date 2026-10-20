CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY,
    notification_type VARCHAR(50) NOT NULL,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id UUID NOT NULL,
    recipient_email VARCHAR(254) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    template_data TEXT NOT NULL,
    sensitive_data BYTEA,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','PROCESSING','SENT','SIMULATED','FAILED','DEAD_LETTER')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP,
    lease_until TIMESTAMP,
    sent_at TIMESTAMP,
    provider_message_id VARCHAR(120),
    provider_idempotency_key VARCHAR(80) NOT NULL UNIQUE,
    last_error VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notification_outbox_dispatch ON notification_outbox (status, next_attempt_at, created_at);
CREATE INDEX idx_notification_outbox_aggregate ON notification_outbox (aggregate_type, aggregate_id);
