package com.elan.repository;

import com.elan.entity.ReservationGuest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationGuestRepository extends JpaRepository<ReservationGuest, UUID> {
    Optional<ReservationGuest> findFirstByEmailIgnoreCaseAndFirstNameIgnoreCaseAndLastNameIgnoreCase(
            String email, String firstName, String lastName);
}
