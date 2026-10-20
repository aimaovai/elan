package com.elan.repository;

import com.elan.entity.ReservationStatusHistory;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationStatusHistoryRepository extends JpaRepository<ReservationStatusHistory, UUID> {
    List<ReservationStatusHistory> findByReservation_IdOrderByChangedAtDesc(UUID reservationId);
}
