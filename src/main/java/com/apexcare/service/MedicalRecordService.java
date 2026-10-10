package com.apexcare.service;

import com.apexcare.dto.MedicalRecordDTO;
import com.apexcare.entity.Doctor;
import com.apexcare.entity.MedicalRecord;
import com.apexcare.entity.Patient;
import com.apexcare.entity.Role;
import com.apexcare.exception.BadRequestException;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.DoctorRepository;
import com.apexcare.repository.MedicalRecordRepository;
import com.apexcare.repository.PatientRepository;
import com.apexcare.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository,
                                PatientRepository patientRepository,
                                DoctorRepository doctorRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordDTO> getByPatientId(Long patientId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.PATIENT) {
            Patient p = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            if (!p.getId().equals(patientId)) {
                throw new BadRequestException("You can only access your own medical records");
            }
        }

        return medicalRecordRepository.findByPatientIdOrderByRecordDateDesc(patientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordDTO> getByDoctorId(Long doctorId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor d = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
            doctorId = d.getId();
        }

        return medicalRecordRepository.findByDoctorIdOrderByRecordDateDesc(doctorId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MedicalRecordDTO getById(Long id, UserPrincipal currentUser) {
        MedicalRecord mr = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found with ID: " + id));

        if (currentUser.getRole() == Role.PATIENT) {
            Patient p = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            if (!mr.getPatient().getId().equals(p.getId())) {
                throw new BadRequestException("You can only access your own medical records");
            }
        }

        return mapToDTO(mr);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordDTO> getAllRecords() {
        return medicalRecordRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public MedicalRecordDTO mapToDTO(MedicalRecord mr) {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setId(mr.getId());
        dto.setPatientId(mr.getPatient().getId());
        dto.setPatientName(mr.getPatient().getUser().getName());
        dto.setPatientCode(mr.getPatient().getPatientCode());
        dto.setDoctorId(mr.getDoctor().getId());
        dto.setDoctorName(mr.getDoctor().getUser().getName());
        dto.setDoctorSpecialization(mr.getDoctor().getSpecialization());
        if (mr.getConsultation() != null) {
            dto.setConsultationId(mr.getConsultation().getId());
        }
        dto.setRecordDate(mr.getRecordDate());
        dto.setDiagnosis(mr.getDiagnosis());
        dto.setTreatment(mr.getTreatment());
        dto.setNotes(mr.getNotes());
        return dto;
    }
}
