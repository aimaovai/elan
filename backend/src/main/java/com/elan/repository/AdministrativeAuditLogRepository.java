package com.elan.repository;

import com.elan.entity.AdministrativeAuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministrativeAuditLogRepository extends JpaRepository<AdministrativeAuditLog, UUID> {}
