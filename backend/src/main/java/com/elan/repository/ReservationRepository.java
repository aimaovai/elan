package com.elan.repository;

import com.elan.entity.Reservation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    @EntityGraph(attributePaths = {"guest", "table", "experience"})
    List<Reservation> findByTable_IdAndReservationDateAndStatusIn(
            UUID tableId, LocalDate date, Set<String> statuses);

    @EntityGraph(attributePaths = {"guest", "table", "experience"})
    Optional<Reservation> findByManagementTokenHash(String managementTokenHash);
}
