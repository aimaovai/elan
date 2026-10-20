package com.elan.repository;

import com.elan.entity.PrivateDiningInquiry;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivateDiningInquiryRepository extends JpaRepository<PrivateDiningInquiry, UUID> {
    @EntityGraph(attributePaths = "space")
    @Query("""
            select i from PrivateDiningInquiry i
            where (:status is null or i.status = :status)
              and (:date is null or i.preferredDate = :date)
              and (:query = '' or lower(i.referenceNumber) like lower(concat('%', :query, '%'))
                   or lower(i.firstName) like lower(concat('%', :query, '%'))
                   or lower(i.lastName) like lower(concat('%', :query, '%')))
            """)
    Page<PrivateDiningInquiry> searchAdmin(@Param("status") String status, @Param("date") LocalDate date,
                                          @Param("query") String query, Pageable pageable);
    long countByStatus(String status);
}
