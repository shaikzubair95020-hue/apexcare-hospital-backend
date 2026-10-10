package com.apexcare.controller;

import com.apexcare.dto.AdminDashboardDTO;
import com.apexcare.dto.DoctorDashboardDTO;
import com.apexcare.dto.PatientDashboardDTO;
import com.apexcare.dto.ReceptionDashboardDTO;
import com.apexcare.security.UserPrincipal;
import com.apexcare.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardDTO> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<DoctorDashboardDTO> getDoctorDashboard(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(dashboardService.getDoctorDashboard(userPrincipal.getId()));
    }

    @GetMapping("/reception")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ReceptionDashboardDTO> getReceptionDashboard() {
        return ResponseEntity.ok(dashboardService.getReceptionDashboard());
    }

    @GetMapping("/patient")
    @PreAuthorize("hasAnyRole('ADMIN', 'PATIENT')")
    public ResponseEntity<PatientDashboardDTO> getPatientDashboard(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(dashboardService.getPatientDashboard(userPrincipal.getId()));
    }
}
