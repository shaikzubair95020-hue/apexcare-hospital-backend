package com.apexcare.service;

import com.apexcare.dto.AppointmentDTO;
import com.apexcare.dto.AppointmentRequest;
import com.apexcare.entity.*;
import com.apexcare.exception.BadRequestException;
import com.apexcare.exception.ConflictException;
import com.apexcare.exception.InvalidAppointmentException;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.*;
import com.apexcare.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final BillRepository billRepository;
    private final ConsultationRepository consultationRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository,
                              DoctorAvailabilityRepository availabilityRepository,
                              BillRepository billRepository,
                              ConsultationRepository consultationRepository,
                              NotificationService notificationService,
                              AuditLogService auditLogService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.availabilityRepository = availabilityRepository;
        this.billRepository = billRepository;
        this.consultationRepository = consultationRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointments(Long doctorId, Long patientId, LocalDate date, String status, UserPrincipal currentUser) {
        // Enforce role-based scoping
        if (currentUser.getRole() == Role.PATIENT) {
            Patient p = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            patientId = p.getId();
        } else if (currentUser.getRole() == Role.DOCTOR) {
            Doctor d = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
            doctorId = d.getId();
        }

        return appointmentRepository.filterAppointments(doctorId, patientId, date, status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AppointmentDTO getAppointmentById(Long id, UserPrincipal currentUser) {
        Appointment app = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        // Authorization check
        if (currentUser.getRole() == Role.PATIENT) {
            Patient p = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            if (!app.getPatient().getId().equals(p.getId())) {
                throw new BadRequestException("You are not authorized to view this appointment");
            }
        } else if (currentUser.getRole() == Role.DOCTOR) {
            Doctor d = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
            if (!app.getDoctor().getId().equals(d.getId())) {
                throw new BadRequestException("You are not authorized to view this appointment");
            }
        }

        return mapToDTO(app);
    }

    @Transactional
    public AppointmentDTO bookAppointment(AppointmentRequest request, UserPrincipal currentUser) {
        Patient patient;
        if (currentUser.getRole() == Role.PATIENT) {
            patient = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user: " + currentUser.getUsername()));
        } else {
            if (request.getPatientId() == null) {
                throw new BadRequestException("Patient ID is required for staff booking");
            }
            patient = patientRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));
        }

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        if (!"ACTIVE".equalsIgnoreCase(doctor.getStatus())) {
            throw new InvalidAppointmentException("The selected doctor is currently inactive and cannot accept appointments");
        }

        if (request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new InvalidAppointmentException("Cannot book appointments for past dates");
        }

        if (request.getAppointmentDate().isEqual(LocalDate.now()) && request.getAppointmentTime().isBefore(LocalTime.now())) {
            throw new InvalidAppointmentException("Cannot book appointment time slots that have already passed");
        }

        // Verify doctor availability on that day of week
        String dayOfWeek = request.getAppointmentDate().getDayOfWeek().name();
        Optional<DoctorAvailability> optAvail = availabilityRepository.findByDoctorIdAndDayOfWeekIgnoreCase(doctor.getId(), dayOfWeek);
        if (optAvail.isEmpty() || !optAvail.get().isAvailable()) {
            throw new InvalidAppointmentException("Doctor is not scheduled for consultations on " + dayOfWeek);
        }

        DoctorAvailability avail = optAvail.get();
        if (request.getAppointmentTime().isBefore(avail.getStartTime()) || request.getAppointmentTime().isAfter(avail.getEndTime().minusMinutes(30))) {
            throw new InvalidAppointmentException("Selected time is outside doctor's consultation hours (" + avail.getStartTime() + " - " + avail.getEndTime() + ")");
        }

        // Business Rule: Check for double booking
        if (appointmentRepository.existsActiveAppointment(doctor.getId(), request.getAppointmentDate(), request.getAppointmentTime())) {
            throw new ConflictException("Doctor already has an active appointment booked for " + request.getAppointmentDate() + " at " + request.getAppointmentTime());
        }

        Appointment appointment = new Appointment(
                patient,
                doctor,
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                request.getReason(),
                "PENDING"
        );
        Appointment saved = appointmentRepository.save(appointment);

        // Generate preliminary bill record with consultation fee
        Bill bill = new Bill(
                patient,
                saved,
                doctor.getConsultationFee() != null ? doctor.getConsultationFee() : BigDecimal.valueOf(500.00),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                saved.getAppointmentDate()
        );
        billRepository.save(bill);

        // Notify patient & doctor
        notificationService.createNotification(
                patient.getUser().getId(),
                "Appointment Booked",
                "Your appointment with Dr. " + doctor.getUser().getName() + " on " + saved.getAppointmentDate() + " at " + saved.getAppointmentTime() + " is pending confirmation.",
                "APPOINTMENT"
        );
        notificationService.createNotification(
                doctor.getUser().getId(),
                "New Appointment Request",
                "Patient " + patient.getUser().getName() + " requested an appointment on " + saved.getAppointmentDate() + " at " + saved.getAppointmentTime(),
                "APPOINTMENT"
        );

        auditLogService.log("BOOK_APPOINTMENT", currentUser.getUsername(), "Appointment", saved.getId(),
                "Booked appointment for " + patient.getUser().getName() + " with Dr. " + doctor.getUser().getName());

        return mapToDTO(saved);
    }

    @Transactional
    public AppointmentDTO confirmAppointment(Long id, String performedBy) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        if ("CANCELLED".equalsIgnoreCase(appointment.getStatus())) {
            throw new BadRequestException("Cannot confirm a cancelled appointment");
        }

        appointment.setStatus("CONFIRMED");
        Appointment updated = appointmentRepository.save(appointment);

        notificationService.createNotification(
                appointment.getPatient().getUser().getId(),
                "Appointment Confirmed",
                "Your appointment with Dr. " + appointment.getDoctor().getUser().getName() + " on " + appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() + " has been confirmed.",
                "APPOINTMENT"
        );

        auditLogService.log("CONFIRM_APPOINTMENT", performedBy, "Appointment", id, "Confirmed appointment ID: " + id);
        return mapToDTO(updated);
    }

    @Transactional
    public AppointmentDTO cancelAppointment(Long id, String performedBy, UserPrincipal currentUser) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        if ("COMPLETED".equalsIgnoreCase(appointment.getStatus())) {
            throw new BadRequestException("Completed appointments cannot be cancelled");
        }

        appointment.setStatus("CANCELLED");
        Appointment updated = appointmentRepository.save(appointment);

        // Cancel associated bill if not already paid
        Optional<Bill> optBill = billRepository.findByAppointmentId(id);
        if (optBill.isPresent()) {
            Bill bill = optBill.get();
            if (!"PAID".equalsIgnoreCase(bill.getStatus())) {
                bill.setStatus("CANCELLED");
                billRepository.save(bill);
            }
        }

        notificationService.createNotification(
                appointment.getPatient().getUser().getId(),
                "Appointment Cancelled",
                "Your appointment with Dr. " + appointment.getDoctor().getUser().getName() + " on " + appointment.getAppointmentDate() + " has been cancelled.",
                "APPOINTMENT"
        );

        notificationService.createNotification(
                appointment.getDoctor().getUser().getId(),
                "Appointment Cancelled",
                "Appointment with patient " + appointment.getPatient().getUser().getName() + " on " + appointment.getAppointmentDate() + " was cancelled.",
                "APPOINTMENT"
        );

        auditLogService.log("CANCEL_APPOINTMENT", performedBy, "Appointment", id, "Cancelled appointment ID: " + id);
        return mapToDTO(updated);
    }

    @Transactional
    public AppointmentDTO completeAppointment(Long id, String performedBy) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        if ("CANCELLED".equalsIgnoreCase(appointment.getStatus())) {
            throw new BadRequestException("Cancelled appointments cannot be marked completed");
        }

        appointment.setStatus("COMPLETED");
        Appointment updated = appointmentRepository.save(appointment);

        auditLogService.log("COMPLETE_APPOINTMENT", performedBy, "Appointment", id, "Completed appointment ID: " + id);
        return mapToDTO(updated);
    }

    public AppointmentDTO mapToDTO(Appointment app) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(app.getId());
        dto.setPatientId(app.getPatient().getId());
        dto.setPatientName(app.getPatient().getUser().getName());
        dto.setPatientCode(app.getPatient().getPatientCode());
        dto.setPatientPhone(app.getPatient().getUser().getPhone());

        dto.setDoctorId(app.getDoctor().getId());
        dto.setDoctorName(app.getDoctor().getUser().getName());
        dto.setDoctorCode(app.getDoctor().getDoctorCode());
        dto.setSpecialization(app.getDoctor().getSpecialization());

        dto.setDepartmentId(app.getDoctor().getDepartment().getId());
        dto.setDepartmentName(app.getDoctor().getDepartment().getName());
        dto.setConsultationFee(app.getDoctor().getConsultationFee());

        dto.setAppointmentDate(app.getAppointmentDate());
        dto.setAppointmentTime(app.getAppointmentTime());
        dto.setReason(app.getReason());
        dto.setStatus(app.getStatus());
        dto.setCreatedAt(app.getCreatedAt());

        dto.setHasConsultation(consultationRepository.findByAppointmentId(app.getId()).isPresent());
        dto.setHasBill(billRepository.findByAppointmentId(app.getId()).isPresent());

        return dto;
    }
}
