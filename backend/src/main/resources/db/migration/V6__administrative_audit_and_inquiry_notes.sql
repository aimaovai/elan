ALTER TABLE reservation_status_history
    ADD COLUMN administrator_id UUID REFERENCES administrators(id);

ALTER TABLE private_dining_inquiries
    ADD COLUMN internal_notes VARCHAR(2000);

CREATE TABLE administrative_audit_log (
    id UUID PRIMARY KEY,
    administrator_id UUID NOT NULL REFERENCES administrators(id),
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    previous_value VARCHAR(1000),
    new_value VARCHAR(1000),
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_admin_audit_entity ON administrative_audit_log (entity_type, entity_id, created_at DESC);
