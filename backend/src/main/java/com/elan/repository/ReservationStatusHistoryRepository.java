package com.elan.repository;

import com.elan.entity.ReservationStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationStatusHistoryRepository extends JpaRepository<ReservationStatusHistory, UUID> {}
