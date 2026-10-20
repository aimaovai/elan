package com.elan.repository;

import com.elan.entity.PrivateDiningSpace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivateDiningSpaceRepository extends JpaRepository<PrivateDiningSpace, UUID> {
    List<PrivateDiningSpace> findByActiveTrueOrderByDisplayOrderAsc();
    Optional<PrivateDiningSpace> findBySlugAndActiveTrue(String slug);
}
