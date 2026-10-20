package com.elan.repository;

import com.elan.entity.RestaurantSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantSettingsRepository extends JpaRepository<RestaurantSettings, UUID> {
    Optional<RestaurantSettings> findFirstByOrderByUpdatedAtDesc();
}
