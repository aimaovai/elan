package com.elan.repository;

import com.elan.entity.RestaurantServicePeriod;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantServicePeriodRepository extends JpaRepository<RestaurantServicePeriod, UUID> {
    List<RestaurantServicePeriod> findByDayOfWeekAndActiveTrueOrderByStartTimeAsc(int dayOfWeek);
}
