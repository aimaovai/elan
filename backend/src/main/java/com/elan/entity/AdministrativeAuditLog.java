package com.elan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "administrative_audit_log")
public class AdministrativeAuditLog {
    @Id public UUID id;
    @Column(name = "administrator_id", nullable = false) public UUID administratorId;
    @Column(name = "entity_type", nullable = false, length = 40) public String entityType;
    @Column(name = "entity_id", nullable = false) public UUID entityId;
    @Column(nullable = false, length = 40) public String action;
    @Column(name = "previous_value", length = 1000) public String previousValue;
    @Column(name = "new_value", length = 1000) public String newValue;
    @Column(length = 500) public String reason;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt;
}
