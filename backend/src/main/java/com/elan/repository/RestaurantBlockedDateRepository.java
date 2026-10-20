package com.elan.repository;

import com.elan.entity.RestaurantBlockedDate;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

public interface RestaurantBlockedDateRepository extends JpaRepository<RestaurantBlockedDate, UUID> {
    List<RestaurantBlockedDate> findByBlockedDate(LocalDate blockedDate);
    List<RestaurantBlockedDate> findByBlockedDateBetween(LocalDate from, LocalDate to, Sort sort);
}
