package com.apexcare.controller;

import com.apexcare.dto.PrescriptionDTO;
import com.apexcare.security.UserPrincipal;
import com.apexcare.service.PrescriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping
    public ResponseEntity<List<PrescriptionDTO>> getPrescriptions(
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (patientId != null) {
            return ResponseEntity.ok(prescriptionService.getByPatientId(patientId, userPrincipal));
        }
        if (doctorId != null) {
            return ResponseEntity.ok(prescriptionService.getByDoctorId(doctorId, userPrincipal));
        }
        return ResponseEntity.ok(prescriptionService.getByPatientId(userPrincipal.getId(), userPrincipal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionDTO> getPrescriptionById(@PathVariable Long id,
                                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionById(id, userPrincipal));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PrescriptionDTO>> getByPatientId(@PathVariable Long patientId,
                                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(prescriptionService.getByPatientId(patientId, userPrincipal));
    }
}
