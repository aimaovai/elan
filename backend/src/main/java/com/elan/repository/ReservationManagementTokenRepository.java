package com.elan.repository;

import com.elan.entity.ReservationManagementToken;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationManagementTokenRepository extends JpaRepository<ReservationManagementToken, UUID> {
    @EntityGraph(attributePaths = {"reservation", "reservation.guest", "reservation.table", "reservation.experience"})
    Optional<ReservationManagementToken> findByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);
    long deleteByExpiresAtBefore(LocalDateTime now);
}
