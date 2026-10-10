package com.apexcare.service;

import com.apexcare.dto.DoctorAvailabilityDTO;
import com.apexcare.dto.DoctorDTO;
import com.apexcare.dto.DoctorRequest;
import com.apexcare.entity.Department;
import com.apexcare.entity.Doctor;
import com.apexcare.entity.DoctorAvailability;
import com.apexcare.entity.Role;
import com.apexcare.entity.User;
import com.apexcare.exception.DuplicateResourceException;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.DepartmentRepository;
import com.apexcare.repository.DoctorAvailabilityRepository;
import com.apexcare.repository.DoctorRepository;
import com.apexcare.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public DoctorService(DoctorRepository doctorRepository,
                         UserRepository userRepository,
                         DepartmentRepository departmentRepository,
                         DoctorAvailabilityRepository availabilityRepository,
                         PasswordEncoder passwordEncoder,
                         AuditLogService auditLogService) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.availabilityRepository = availabilityRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<DoctorDTO> getAllDoctors(Long deptId, String status, String query) {
        return doctorRepository.searchDoctors(deptId, status, query).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DoctorDTO getDoctorById(Long id) {
        Doctor doc = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));
        return mapToDTO(doc);
    }

    @Transactional(readOnly = true)
    public DoctorDTO getDoctorByUserId(Long userId) {
        Doctor doc = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user ID: " + userId));
        return mapToDTO(doc);
    }

    @Transactional
    public DoctorDTO createDoctor(DoctorRequest request, String performedBy) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        String password = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword() : "Doctor@123";

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(password),
                request.getPhone(),
                Role.DOCTOR
        );
        User savedUser = userRepository.save(user);

        String code = request.getDoctorCode();
        if (code == null || code.isBlank()) {
            code = "DOC-" + (1000 + doctorRepository.count() + 1);
        }

        Doctor doctor = new Doctor(
                savedUser,
                code,
                request.getSpecialization(),
                request.getQualification(),
                request.getExperience(),
                request.getLicenseNumber(),
                department,
                request.getConsultationFee()
        );
        if (request.getStatus() != null) {
            doctor.setStatus(request.getStatus());
        }

        Doctor savedDoctor = doctorRepository.save(doctor);

        // Seed standard availability Mon-Fri 09:00 - 17:00
        List<String> defaultDays = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
        for (String day : defaultDays) {
            DoctorAvailability avail = new DoctorAvailability(
                    savedDoctor,
                    day,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    true
            );
            availabilityRepository.save(avail);
        }

        auditLogService.log("CREATE_DOCTOR", performedBy, "Doctor", savedDoctor.getId(), "Created doctor " + savedDoctor.getUser().getName());
        return mapToDTO(savedDoctor);
    }

    @Transactional
    public DoctorDTO updateDoctor(Long id, DoctorRequest request, String performedBy) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        User user = doctor.getUser();
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(user);

        doctor.setSpecialization(request.getSpecialization());
        doctor.setQualification(request.getQualification());
        doctor.setExperience(request.getExperience());
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setDepartment(department);
        doctor.setConsultationFee(request.getConsultationFee());
        if (request.getStatus() != null) {
            doctor.setStatus(request.getStatus());
        }

        Doctor updated = doctorRepository.save(doctor);
        auditLogService.log("UPDATE_DOCTOR", performedBy, "Doctor", updated.getId(), "Updated doctor " + updated.getUser().getName());
        return mapToDTO(updated);
    }

    @Transactional
    public void deleteDoctor(Long id, String performedBy) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));
        doctor.setStatus("INACTIVE");
        doctor.getUser().setEnabled(false);
        userRepository.save(doctor.getUser());
        doctorRepository.save(doctor);
        auditLogService.log("DEACTIVATE_DOCTOR", performedBy, "Doctor", id, "Deactivated doctor " + doctor.getUser().getName());
    }

    public DoctorDTO mapToDTO(Doctor doctor) {
        DoctorDTO dto = new DoctorDTO();
        dto.setId(doctor.getId());
        dto.setUserId(doctor.getUser().getId());
        dto.setDoctorCode(doctor.getDoctorCode());
        dto.setName(doctor.getUser().getName());
        dto.setEmail(doctor.getUser().getEmail());
        dto.setPhone(doctor.getUser().getPhone());
        dto.setSpecialization(doctor.getSpecialization());
        dto.setQualification(doctor.getQualification());
        dto.setExperience(doctor.getExperience());
        dto.setLicenseNumber(doctor.getLicenseNumber());
        dto.setDepartmentId(doctor.getDepartment().getId());
        dto.setDepartmentName(doctor.getDepartment().getName());
        dto.setConsultationFee(doctor.getConsultationFee());
        dto.setStatus(doctor.getStatus());

        List<DoctorAvailabilityDTO> availList = availabilityRepository.findByDoctorId(doctor.getId()).stream()
                .map(a -> new DoctorAvailabilityDTO(a.getId(), doctor.getId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime(), a.isAvailable()))
                .collect(Collectors.toList());
        dto.setAvailabilities(availList);

        return dto;
    }
}
