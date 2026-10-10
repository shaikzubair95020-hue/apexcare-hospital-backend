package com.apexcare.controller;

import com.apexcare.dto.PatientDTO;
import com.apexcare.dto.PatientRequest;
import com.apexcare.security.UserPrincipal;
import com.apexcare.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<List<PatientDTO>> getAllPatients(@RequestParam(value = "query", required = false) String query) {
        return ResponseEntity.ok(patientService.getAllPatients(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<PatientDTO> getPatientById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<PatientDTO> createPatient(@Valid @RequestBody PatientRequest request,
                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(patientService.createPatient(request, userPrincipal.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<PatientDTO> updatePatient(@PathVariable Long id,
                                                    @Valid @RequestBody PatientRequest request,
                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(patientService.updatePatient(id, request, userPrincipal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        patientService.deletePatient(id, userPrincipal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
