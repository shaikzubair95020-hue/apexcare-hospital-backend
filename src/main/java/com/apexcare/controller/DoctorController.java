package com.apexcare.controller;

import com.apexcare.dto.DoctorAvailabilityDTO;
import com.apexcare.dto.DoctorDTO;
import com.apexcare.dto.DoctorRequest;
import com.apexcare.dto.TimeSlotDTO;
import com.apexcare.security.UserPrincipal;
import com.apexcare.service.DoctorAvailabilityService;
import com.apexcare.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final DoctorAvailabilityService availabilityService;

    public DoctorController(DoctorService doctorService, DoctorAvailabilityService availabilityService) {
        this.doctorService = doctorService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public ResponseEntity<List<DoctorDTO>> getAllDoctors(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "query", required = false) String query) {
        return ResponseEntity.ok(doctorService.getAllDoctors(departmentId, status, query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorDTO> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorDTO> createDoctor(@Valid @RequestBody DoctorRequest request,
                                                  @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(doctorService.createDoctor(request, userPrincipal.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<DoctorDTO> updateDoctor(@PathVariable Long id,
                                                  @Valid @RequestBody DoctorRequest request,
                                                  @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(doctorService.updateDoctor(id, request, userPrincipal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal userPrincipal) {
        doctorService.deleteDoctor(id, userPrincipal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{doctorId}/availability")
    public ResponseEntity<List<DoctorAvailabilityDTO>> getAvailability(@PathVariable Long doctorId) {
        return ResponseEntity.ok(availabilityService.getAvailabilityByDoctor(doctorId));
    }

    @PostMapping("/{doctorId}/availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<DoctorAvailabilityDTO> updateAvailability(@PathVariable Long doctorId,
                                                                    @RequestBody DoctorAvailabilityDTO dto) {
        return ResponseEntity.ok(availabilityService.saveOrUpdateAvailability(
                doctorId, dto.getDayOfWeek(), dto.getStartTime(), dto.getEndTime(), dto.isAvailable()
        ));
    }

    @GetMapping("/{doctorId}/slots")
    public ResponseEntity<List<TimeSlotDTO>> getAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.getAvailableSlots(doctorId, date));
    }
}
