package com.elan.controller;

import com.elan.dto.ReservationDtos.PrivateDiningInquiryRequest;
import com.elan.dto.ReservationDtos.PrivateDiningInquiryResponse;
import com.elan.service.PrivateDiningInquiryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/private-dining/inquiries")
public class PrivateDiningInquiryController {
    private final PrivateDiningInquiryService inquiries;

    public PrivateDiningInquiryController(PrivateDiningInquiryService inquiries) {
        this.inquiries = inquiries;
    }

    @PostMapping
    @Operation(summary = "Submit a private dining inquiry; this does not book an event")
    public ResponseEntity<PrivateDiningInquiryResponse> create(
            @Valid @RequestBody PrivateDiningInquiryRequest request) {
        return ResponseEntity.accepted().body(inquiries.create(request));
    }
}
