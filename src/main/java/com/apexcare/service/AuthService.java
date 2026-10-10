package com.apexcare.service;

import com.apexcare.dto.AuthResponse;
import com.apexcare.dto.LoginRequest;
import com.apexcare.dto.RegisterRequest;
import com.apexcare.entity.Doctor;
import com.apexcare.entity.Patient;
import com.apexcare.entity.Role;
import com.apexcare.entity.User;
import com.apexcare.exception.DuplicateResourceException;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.DoctorRepository;
import com.apexcare.repository.PatientRepository;
import com.apexcare.repository.UserRepository;
import com.apexcare.security.JwtTokenProvider;
import com.apexcare.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       DoctorRepository doctorRepository,
                       PatientRepository patientRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long entityId = null;
        String code = null;

        if (user.getRole() == Role.DOCTOR) {
            Optional<Doctor> doc = doctorRepository.findByUserId(user.getId());
            if (doc.isPresent()) {
                entityId = doc.get().getId();
                code = doc.get().getDoctorCode();
            }
        } else if (user.getRole() == Role.PATIENT) {
            Optional<Patient> pat = patientRepository.findByUserId(user.getId());
            if (pat.isPresent()) {
                entityId = pat.get().getId();
                code = pat.get().getPatientCode();
            }
        }

        auditLogService.log("USER_LOGIN", user.getEmail(), "User", user.getId(), "User logged in with role " + user.getRole());

        return new AuthResponse(jwt, user.getId(), user.getEmail(), user.getName(), user.getRole(), entityId, code);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account already exists with email: " + request.getEmail());
        }

        Role role = request.getRole() != null ? request.getRole() : Role.PATIENT;

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getPhone(),
                role
        );
        User savedUser = userRepository.save(user);

        Long entityId = null;
        String code = null;

        if (role == Role.PATIENT) {
            code = "PAT-" + (1000 + patientRepository.count() + 1);
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
            Patient savedPatient = patientRepository.save(patient);
            entityId = savedPatient.getId();
        }

        // Authenticate immediately
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        auditLogService.log("USER_REGISTER", savedUser.getEmail(), "User", savedUser.getId(), "Registered new " + role.name());

        return new AuthResponse(jwt, savedUser.getId(), savedUser.getEmail(), savedUser.getName(), role, entityId, code);
    }

    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser(UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long entityId = null;
        String code = null;

        if (user.getRole() == Role.DOCTOR) {
            Optional<Doctor> doc = doctorRepository.findByUserId(user.getId());
            if (doc.isPresent()) {
                entityId = doc.get().getId();
                code = doc.get().getDoctorCode();
            }
        } else if (user.getRole() == Role.PATIENT) {
            Optional<Patient> pat = patientRepository.findByUserId(user.getId());
            if (pat.isPresent()) {
                entityId = pat.get().getId();
                code = pat.get().getPatientCode();
            }
        }

        return new AuthResponse(null, user.getId(), user.getEmail(), user.getName(), user.getRole(), entityId, code);
    }
}
