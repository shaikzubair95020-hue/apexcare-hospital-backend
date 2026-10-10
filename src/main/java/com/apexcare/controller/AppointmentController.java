package com.apexcare.controller;

import com.apexcare.dto.AppointmentDTO;
import com.apexcare.dto.AppointmentRequest;
import com.apexcare.security.UserPrincipal;
import com.apexcare.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getAppointments(
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "status", required = false) String status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(appointmentService.getAppointments(doctorId, patientId, date, status, userPrincipal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDTO> getAppointmentById(@PathVariable Long id,
                                                             @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id, userPrincipal));
    }

    @PostMapping
    public ResponseEntity<AppointmentDTO> bookAppointment(@Valid @RequestBody AppointmentRequest request,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(appointmentService.bookAppointment(request, userPrincipal));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<AppointmentDTO> confirmAppointment(@PathVariable Long id,
                                                             @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(appointmentService.confirmAppointment(id, userPrincipal.getUsername()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AppointmentDTO> cancelAppointment(@PathVariable Long id,
                                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, userPrincipal.getUsername(), userPrincipal));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<AppointmentDTO> completeAppointment(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(appointmentService.completeAppointment(id, userPrincipal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserPrincipal userPrincipal) {
        appointmentService.cancelAppointment(id, userPrincipal.getUsername(), userPrincipal);
        return ResponseEntity.noContent().build();
    }
}
