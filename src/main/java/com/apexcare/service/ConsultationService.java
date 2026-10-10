package com.apexcare.service;

import com.apexcare.dto.*;
import com.apexcare.entity.*;
import com.apexcare.exception.BadRequestException;
import com.apexcare.exception.ConflictException;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.*;
import com.apexcare.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public ConsultationService(ConsultationRepository consultationRepository,
                               AppointmentRepository appointmentRepository,
                               DoctorRepository doctorRepository,
                               PatientRepository patientRepository,
                               PrescriptionRepository prescriptionRepository,
                               MedicalRecordRepository medicalRecordRepository,
                               NotificationService notificationService,
                               AuditLogService auditLogService) {
        this.consultationRepository = consultationRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ConsultationDTO createConsultation(ConsultationRequest request, UserPrincipal currentUser) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + request.getAppointmentId()));

        if ("CANCELLED".equalsIgnoreCase(appointment.getStatus())) {
            throw new BadRequestException("Cannot create consultation for a cancelled appointment");
        }

        if (consultationRepository.findByAppointmentId(appointment.getId()).isPresent()) {
            throw new ConflictException("A consultation has already been recorded for this appointment");
        }

        // Verify doctor authorization
        Doctor doctor = appointment.getDoctor();
        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor currentDoc = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
            if (!doctor.getId().equals(currentDoc.getId())) {
                throw new BadRequestException("You are not authorized to conduct this appointment's consultation");
            }
        }

        Patient patient = appointment.getPatient();
        LocalDate consultDate = request.getConsultationDate() != null ? request.getConsultationDate() : LocalDate.now();

        Consultation consultation = new Consultation(
                appointment,
                patient,
                doctor,
                request.getSymptoms(),
                request.getDiagnosis(),
                request.getTreatment(),
                request.getNotes(),
                consultDate
        );
        Consultation savedConsultation = consultationRepository.save(consultation);

        // Mark appointment as COMPLETED
        appointment.setStatus("COMPLETED");
        appointmentRepository.save(appointment);

        // Auto-create Medical Record for patient historical timeline
        MedicalRecord medicalRecord = new MedicalRecord(
                patient,
                doctor,
                savedConsultation,
                consultDate,
                request.getDiagnosis(),
                request.getTreatment(),
                request.getNotes()
        );
        medicalRecordRepository.save(medicalRecord);

        // Handle prescription creation if items supplied
        Prescription savedPrescription = null;
        if (request.getPrescriptionItems() != null && !request.getPrescriptionItems().isEmpty()) {
            Prescription prescription = new Prescription(
                    savedConsultation,
                    patient,
                    doctor,
                    consultDate,
                    request.getPrescriptionInstructions()
            );
            for (PrescriptionItemDTO itemDto : request.getPrescriptionItems()) {
                PrescriptionItem item = new PrescriptionItem(
                        itemDto.getMedicineName(),
                        itemDto.getDosage(),
                        itemDto.getFrequency(),
                        itemDto.getDuration(),
                        itemDto.getInstructions()
                );
                prescription.addItem(item);
            }
            savedPrescription = prescriptionRepository.save(prescription);
        }

        // Send patient notification
        notificationService.createNotification(
                patient.getUser().getId(),
                "Consultation Completed",
                "Dr. " + doctor.getUser().getName() + " completed your consultation. Diagnosis and records have been updated.",
                "CONSULTATION"
        );

        auditLogService.log("CREATE_CONSULTATION", currentUser.getUsername(), "Consultation", savedConsultation.getId(),
                "Completed consultation for " + patient.getUser().getName() + " with Dr. " + doctor.getUser().getName());

        return mapToDTO(savedConsultation, savedPrescription);
    }

    @Transactional(readOnly = true)
    public ConsultationDTO getConsultationById(Long id, UserPrincipal currentUser) {
        Consultation consult = consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with ID: " + id));

        validateAccess(consult.getPatient().getId(), currentUser);
        return mapToDTO(consult, prescriptionRepository.findByConsultationId(consult.getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public ConsultationDTO getByAppointmentId(Long appointmentId, UserPrincipal currentUser) {
        Consultation consult = consultationRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found for appointment: " + appointmentId));

        validateAccess(consult.getPatient().getId(), currentUser);
        return mapToDTO(consult, prescriptionRepository.findByConsultationId(consult.getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ConsultationDTO> getByPatientId(Long patientId, UserPrincipal currentUser) {
        validateAccess(patientId, currentUser);
        return consultationRepository.findByPatientIdOrderByConsultationDateDesc(patientId).stream()
                .map(c -> mapToDTO(c, prescriptionRepository.findByConsultationId(c.getId()).orElse(null)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConsultationDTO> getByDoctorId(Long doctorId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor d = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
            doctorId = d.getId();
        }
        return consultationRepository.findByDoctorIdOrderByConsultationDateDesc(doctorId).stream()
                .map(c -> mapToDTO(c, prescriptionRepository.findByConsultationId(c.getId()).orElse(null)))
                .collect(Collectors.toList());
    }

    private void validateAccess(Long patientId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.PATIENT) {
            Patient p = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            if (!p.getId().equals(patientId)) {
                throw new BadRequestException("You can only access your own medical records");
            }
        }
    }

    public ConsultationDTO mapToDTO(Consultation c, Prescription p) {
        ConsultationDTO dto = new ConsultationDTO();
        dto.setId(c.getId());
        dto.setAppointmentId(c.getAppointment().getId());
        dto.setPatientId(c.getPatient().getId());
        dto.setPatientName(c.getPatient().getUser().getName());
        dto.setPatientCode(c.getPatient().getPatientCode());

        dto.setDoctorId(c.getDoctor().getId());
        dto.setDoctorName(c.getDoctor().getUser().getName());
        dto.setDoctorSpecialization(c.getDoctor().getSpecialization());

        dto.setSymptoms(c.getSymptoms());
        dto.setDiagnosis(c.getDiagnosis());
        dto.setTreatment(c.getTreatment());
        dto.setNotes(c.getNotes());
        dto.setConsultationDate(c.getConsultationDate());
        dto.setCreatedAt(c.getCreatedAt());

        if (p != null) {
            PrescriptionDTO pDto = new PrescriptionDTO();
            pDto.setId(p.getId());
            pDto.setConsultationId(c.getId());
            pDto.setAppointmentId(c.getAppointment().getId());
            pDto.setPatientId(p.getPatient().getId());
            pDto.setPatientName(p.getPatient().getUser().getName());
            pDto.setPatientCode(p.getPatient().getPatientCode());
            pDto.setDoctorId(p.getDoctor().getId());
            pDto.setDoctorName(p.getDoctor().getUser().getName());
            pDto.setDoctorSpecialization(p.getDoctor().getSpecialization());
            pDto.setPrescriptionDate(p.getPrescriptionDate());
            pDto.setInstructions(p.getInstructions());
            pDto.setCreatedAt(p.getCreatedAt());

            List<PrescriptionItemDTO> itemDtos = p.getItems().stream()
                    .map(it -> new PrescriptionItemDTO(it.getId(), it.getMedicineName(), it.getDosage(), it.getFrequency(), it.getDuration(), it.getInstructions()))
                    .collect(Collectors.toList());
            pDto.setItems(itemDtos);
            dto.setPrescription(pDto);
        }

        return dto;
    }
}
