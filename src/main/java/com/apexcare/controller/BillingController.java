package com.apexcare.controller;

import com.apexcare.dto.BillDTO;
import com.apexcare.dto.BillRequest;
import com.apexcare.security.UserPrincipal;
import com.apexcare.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    public ResponseEntity<List<BillDTO>> getAllBills(
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "status", required = false) String status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(billingService.getAllBills(patientId, status, userPrincipal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillDTO> getBillById(@PathVariable Long id,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(billingService.getBillById(id, userPrincipal));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<BillDTO> createBill(@Valid @RequestBody BillRequest request,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(billingService.createBill(request, userPrincipal.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<BillDTO> updateBill(@PathVariable Long id,
                                              @Valid @RequestBody BillRequest request,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(billingService.updateBill(id, request, userPrincipal.getUsername()));
    }
}
