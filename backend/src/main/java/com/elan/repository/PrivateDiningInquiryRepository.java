package com.elan.repository;

import com.elan.entity.PrivateDiningInquiry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivateDiningInquiryRepository extends JpaRepository<PrivateDiningInquiry, UUID> {}
