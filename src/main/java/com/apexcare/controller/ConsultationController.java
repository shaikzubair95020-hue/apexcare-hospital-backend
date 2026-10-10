package com.apexcare.controller;

import com.apexcare.dto.ConsultationDTO;
import com.apexcare.dto.ConsultationRequest;
import com.apexcare.security.UserPrincipal;
import com.apexcare.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping
    public ResponseEntity<List<ConsultationDTO>> getConsultations(
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (patientId != null) {
            return ResponseEntity.ok(consultationService.getByPatientId(patientId, userPrincipal));
        }
        if (doctorId != null) {
            return ResponseEntity.ok(consultationService.getByDoctorId(doctorId, userPrincipal));
        }
        return ResponseEntity.ok(consultationService.getByDoctorId(null, userPrincipal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsultationDTO> getConsultationById(@PathVariable Long id,
                                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(consultationService.getConsultationById(id, userPrincipal));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ConsultationDTO> getByAppointmentId(@PathVariable Long appointmentId,
                                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(consultationService.getByAppointmentId(appointmentId, userPrincipal));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<ConsultationDTO> createConsultation(@Valid @RequestBody ConsultationRequest request,
                                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(consultationService.createConsultation(request, userPrincipal));
    }
}
