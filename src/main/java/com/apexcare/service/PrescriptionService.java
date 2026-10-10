package com.apexcare.service;

import com.apexcare.dto.PrescriptionDTO;
import com.apexcare.dto.PrescriptionItemDTO;
import com.apexcare.entity.*;
import com.apexcare.exception.BadRequestException;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.*;
import com.apexcare.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               ConsultationRepository consultationRepository,
                               PatientRepository patientRepository,
                               DoctorRepository doctorRepository,
                               NotificationService notificationService,
                               AuditLogService auditLogService) {
        this.prescriptionRepository = prescriptionRepository;
        this.consultationRepository = consultationRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PrescriptionDTO getPrescriptionById(Long id, UserPrincipal currentUser) {
        Prescription p = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));

        validateAccess(p.getPatient().getId(), currentUser);
        return mapToDTO(p);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDTO> getByPatientId(Long patientId, UserPrincipal currentUser) {
        validateAccess(patientId, currentUser);
        return prescriptionRepository.findByPatientIdOrderByPrescriptionDateDesc(patientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDTO> getByDoctorId(Long doctorId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor d = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
            doctorId = d.getId();
        }
        return prescriptionRepository.findByDoctorIdOrderByPrescriptionDateDesc(doctorId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private void validateAccess(Long patientId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.PATIENT) {
            Patient p = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            if (!p.getId().equals(patientId)) {
                throw new BadRequestException("You are only permitted to view your own prescriptions");
            }
        }
    }

    public PrescriptionDTO mapToDTO(Prescription p) {
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setId(p.getId());
        dto.setConsultationId(p.getConsultation().getId());
        dto.setAppointmentId(p.getConsultation().getAppointment().getId());
        dto.setPatientId(p.getPatient().getId());
        dto.setPatientName(p.getPatient().getUser().getName());
        dto.setPatientCode(p.getPatient().getPatientCode());
        dto.setDoctorId(p.getDoctor().getId());
        dto.setDoctorName(p.getDoctor().getUser().getName());
        dto.setDoctorSpecialization(p.getDoctor().getSpecialization());
        dto.setPrescriptionDate(p.getPrescriptionDate());
        dto.setInstructions(p.getInstructions());
        dto.setCreatedAt(p.getCreatedAt());

        List<PrescriptionItemDTO> items = p.getItems().stream()
                .map(it -> new PrescriptionItemDTO(it.getId(), it.getMedicineName(), it.getDosage(), it.getFrequency(), it.getDuration(), it.getInstructions()))
                .collect(Collectors.toList());
        dto.setItems(items);
        return dto;
    }
}
