package com.elan.repository;

import com.elan.entity.ReservationIdempotencyRecord;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationIdempotencyRecordRepository
        extends JpaRepository<ReservationIdempotencyRecord, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"reservation", "reservation.table", "reservation.experience"})
    @Query("select r from ReservationIdempotencyRecord r where r.keyHash = :keyHash")
    Optional<ReservationIdempotencyRecord> lockByKeyHash(@Param("keyHash") String keyHash);

    long deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
