package com.elan.repository;

import com.elan.entity.DiningTable;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface DiningTableRepository extends JpaRepository<DiningTable, UUID> {
    List<DiningTable> findByActiveTrueOrderByMaximumCapacityAscTableNumberAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where t.active = true order by t.id")
    List<DiningTable> lockAllActiveTables();

    @Query(value = "select dining_table_id from dining_experience_table_restrictions where experience_id = :experienceId",
            nativeQuery = true)
    List<UUID> findRestrictedTableIds(@Param("experienceId") UUID experienceId);
}
