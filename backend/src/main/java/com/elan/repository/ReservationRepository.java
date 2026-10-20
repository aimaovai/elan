package com.elan.repository;

import com.elan.entity.Reservation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    @EntityGraph(attributePaths = {"guest", "table", "experience"})
    List<Reservation> findByTable_IdAndReservationDateAndStatusIn(
            UUID tableId, LocalDate date, Set<String> statuses);

    @EntityGraph(attributePaths = {"guest", "table", "experience"})
    Optional<Reservation> findByManagementTokenHash(String managementTokenHash);
    Optional<Reservation> findByConfirmationCode(String confirmationCode);

    @EntityGraph(attributePaths = {"guest", "table", "experience"})
    @Query("""
            select r from Reservation r
            where (:date is null or r.reservationDate = :date)
              and (:status is null or r.status = :status)
              and (:query = '' or
                   lower(r.confirmationCode) like lower(concat('%', :query, '%')) or
                   lower(r.guest.firstName) like lower(concat('%', :query, '%')) or
                   lower(r.guest.lastName) like lower(concat('%', :query, '%')))
            """)
    Page<Reservation> searchAdmin(@Param("date") LocalDate date, @Param("status") String status,
                                  @Param("query") String query, Pageable pageable);

    long countByReservationDateAndStatusIn(LocalDate date, Set<String> statuses);
    long countByReservationDate(LocalDate date);
    long countByReservationDateAndStatus(LocalDate date, String status);
    long countByStatusAndReservationDateGreaterThanEqual(String status, LocalDate date);
    long countByExperience_IdAndReservationDateGreaterThanEqualAndStatusIn(
            UUID experienceId, LocalDate date, Set<String> statuses);

    @EntityGraph(attributePaths = {"guest", "table", "experience"})
    List<Reservation> findByReservationDateOrderByStartTimeAsc(LocalDate date);

    @EntityGraph(attributePaths = {"guest", "table", "experience"})
    List<Reservation> findByReservationDateAndStatusIn(LocalDate date, Set<String> statuses);

    @EntityGraph(attributePaths = {"guest", "table", "experience"})
    List<Reservation> findByReservationDateGreaterThanEqualAndStatusIn(LocalDate date, Set<String> statuses);

    @EntityGraph(attributePaths = {"guest", "table", "experience"})
    List<Reservation> findTop10ByReservationDateGreaterThanEqualOrderByReservationDateAscStartTimeAsc(LocalDate date);
}
