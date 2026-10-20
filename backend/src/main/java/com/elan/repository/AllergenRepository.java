package com.elan.repository;

import com.elan.entity.Allergen;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergenRepository extends JpaRepository<Allergen, UUID> {
    Optional<Allergen> findByNameIgnoreCase(String name);
}
