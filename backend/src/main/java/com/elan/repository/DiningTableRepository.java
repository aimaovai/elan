package com.elan.repository;

import com.elan.entity.DiningTable;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.time.LocalDate;
import java.util.Set;
import com.elan.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface DiningTableRepository extends JpaRepository<DiningTable, UUID> {
    List<DiningTable> findByActiveTrueOrderByMaximumCapacityAscTableNumberAsc();
    Optional<DiningTable> findByTableNumberIgnoreCase(String tableNumber);

    @Query("""
            select r from Reservation r
            join fetch r.guest
            join fetch r.table
            left join fetch r.experience
            where r.table.id = :tableId and r.reservationDate >= :date and r.status in :statuses
            """)
    List<Reservation> findUpcomingReservations(@Param("tableId") UUID tableId, @Param("date") LocalDate date,
                                               @Param("statuses") Set<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where t.active = true order by t.id")
    List<DiningTable> lockAllActiveTables();

    @Query(value = "select dining_table_id from dining_experience_table_restrictions where experience_id = :experienceId",
            nativeQuery = true)
    List<UUID> findRestrictedTableIds(@Param("experienceId") UUID experienceId);
}
