package com.apexcare.service;

import com.apexcare.dto.PatientDTO;
import com.apexcare.dto.PatientRequest;
import com.apexcare.entity.Patient;
import com.apexcare.entity.Role;
import com.apexcare.entity.User;
import com.apexcare.exception.DuplicateResourceException;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.PatientRepository;
import com.apexcare.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public PatientService(PatientRepository patientRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuditLogService auditLogService) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<PatientDTO> getAllPatients(String query) {
        return patientRepository.searchPatients(query).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PatientDTO getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));
        return mapToDTO(patient);
    }

    @Transactional(readOnly = true)
    public PatientDTO getPatientByUserId(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user ID: " + userId));
        return mapToDTO(patient);
    }

    @Transactional
    public PatientDTO createPatient(PatientRequest request, String performedBy) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        String password = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword() : "Patient@123";

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(password),
                request.getPhone(),
                Role.PATIENT
        );
        User savedUser = userRepository.save(user);

        String code = request.getPatientCode();
        if (code == null || code.isBlank()) {
            code = "PAT-" + (1000 + patientRepository.count() + 1);
        }

        Patient patient = new Patient(
                savedUser,
                code,
                request.getDateOfBirth(),
                request.getGender(),
                request.getBloodGroup(),
                request.getAddress(),
                request.getEmergencyContact(),
                request.getEmergencyPhone()
        );
        if (request.getStatus() != null) {
            patient.setStatus(request.getStatus());
        }

        Patient savedPatient = patientRepository.save(patient);
        auditLogService.log("REGISTER_PATIENT", performedBy, "Patient", savedPatient.getId(), "Registered patient " + savedPatient.getUser().getName());
        return mapToDTO(savedPatient);
    }

    @Transactional
    public PatientDTO updatePatient(Long id, PatientRequest request, String performedBy) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));

        User user = patient.getUser();
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(user);

        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setAddress(request.getAddress());
        patient.setEmergencyContact(request.getEmergencyContact());
        patient.setEmergencyPhone(request.getEmergencyPhone());
        if (request.getStatus() != null) {
            patient.setStatus(request.getStatus());
        }

        Patient updated = patientRepository.save(patient);
        auditLogService.log("UPDATE_PATIENT", performedBy, "Patient", updated.getId(), "Updated patient " + updated.getUser().getName());
        return mapToDTO(updated);
    }

    @Transactional
    public void deletePatient(Long id, String performedBy) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));
        patient.setStatus("INACTIVE");
        patient.getUser().setEnabled(false);
        userRepository.save(patient.getUser());
        patientRepository.save(patient);
        auditLogService.log("DEACTIVATE_PATIENT", performedBy, "Patient", id, "Deactivated patient " + patient.getUser().getName());
    }

    public PatientDTO mapToDTO(Patient patient) {
        PatientDTO dto = new PatientDTO();
        dto.setId(patient.getId());
        dto.setUserId(patient.getUser().getId());
        dto.setPatientCode(patient.getPatientCode());
        dto.setName(patient.getUser().getName());
        dto.setEmail(patient.getUser().getEmail());
        dto.setPhone(patient.getUser().getPhone());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setGender(patient.getGender());
        dto.setBloodGroup(patient.getBloodGroup());
        dto.setAddress(patient.getAddress());
        dto.setEmergencyContact(patient.getEmergencyContact());
        dto.setEmergencyPhone(patient.getEmergencyPhone());
        dto.setStatus(patient.getStatus());
        dto.setCreatedAt(patient.getCreatedAt());
        return dto;
    }
}
