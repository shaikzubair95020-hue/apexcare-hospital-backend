package com.apexcare.config;

import com.apexcare.entity.*;
import com.apexcare.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           DoctorRepository doctorRepository,
                           DoctorAvailabilityRepository availabilityRepository,
                           PatientRepository patientRepository,
                           AppointmentRepository appointmentRepository,
                           ConsultationRepository consultationRepository,
                           PrescriptionRepository prescriptionRepository,
                           MedicalRecordRepository medicalRecordRepository,
                           BillRepository billRepository,
                           PaymentRepository paymentRepository,
                           NotificationRepository notificationRepository,
                           AuditLogRepository auditLogRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.availabilityRepository = availabilityRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Data already seeded
        }

        System.out.println(">>> Initializing ApexCare Hospital Management System Database Seed Data...");

        // 1. Seed Departments
        Department cardiology = departmentRepository.save(new Department("Cardiology", "Specialized cardiac care, diagnostics, echocardiograms, and intervention.", "ACTIVE"));
        Department neurology = departmentRepository.save(new Department("Neurology", "Comprehensive neurological and stroke care, EEG, and neural health.", "ACTIVE"));
        Department orthopedics = departmentRepository.save(new Department("Orthopedics", "Bone, joint, spine disorders, sports medicine, and reconstructive surgery.", "ACTIVE"));
        Department pediatrics = departmentRepository.save(new Department("Pediatrics", "Neonatal, child healthcare, vaccinations, and pediatric emergency care.", "ACTIVE"));
        Department dermatology = departmentRepository.save(new Department("Dermatology", "Clinical dermatology, skin disorders, and aesthetic treatments.", "ACTIVE"));
        Department generalMedicine = departmentRepository.save(new Department("General Medicine", "Primary care, health evaluations, and internal medicine.", "ACTIVE"));
        Department ent = departmentRepository.save(new Department("ENT", "Ear, nose, throat, head and neck clinical care and diagnostics.", "ACTIVE"));
        Department gynecology = departmentRepository.save(new Department("Gynecology", "Maternity, obstetrics, women's wellness and reproductive health.", "ACTIVE"));

        // 2. Seed Users
        // Admin
        User admin = userRepository.save(new User(
                "Hospital Administrator",
                "admin@apexcare.com",
                passwordEncoder.encode("Admin@123"),
                "+1-800-555-0199",
                Role.ADMIN
        ));

        // Receptionist
        User receptionist = userRepository.save(new User(
                "Front Desk Receptionist",
                "reception@apexcare.com",
                passwordEncoder.encode("Reception@123"),
                "+1-800-555-0188",
                Role.RECEPTIONIST
        ));

        // Doctors
        User docUser1 = userRepository.save(new User(
                "Dr. Rahul Sharma",
                "doctor@apexcare.com",
                passwordEncoder.encode("Doctor@123"),
                "+1-800-555-0101",
                Role.DOCTOR
        ));

        User docUser2 = userRepository.save(new User(
                "Dr. Priya Patel",
                "dr.priya@apexcare.com",
                passwordEncoder.encode("Doctor@123"),
                "+1-800-555-0102",
                Role.DOCTOR
        ));

        User docUser3 = userRepository.save(new User(
                "Dr. Ananya Roy",
                "dr.ananya@apexcare.com",
                passwordEncoder.encode("Doctor@123"),
                "+1-800-555-0103",
                Role.DOCTOR
        ));

        User docUser4 = userRepository.save(new User(
                "Dr. Vikram Malhotra",
                "dr.vikram@apexcare.com",
                passwordEncoder.encode("Doctor@123"),
                "+1-800-555-0104",
                Role.DOCTOR
        ));

        // Patients
        User patUser1 = userRepository.save(new User(
                "John Doe",
                "patient@apexcare.com",
                passwordEncoder.encode("Patient@123"),
                "+1-555-321-4567",
                Role.PATIENT
        ));

        User patUser2 = userRepository.save(new User(
                "Sarah Jenkins",
                "sarah@apexcare.com",
                passwordEncoder.encode("Patient@123"),
                "+1-555-987-6543",
                Role.PATIENT
        ));

        User patUser3 = userRepository.save(new User(
                "Robert Taylor",
                "robert@apexcare.com",
                passwordEncoder.encode("Patient@123"),
                "+1-555-456-7890",
                Role.PATIENT
        ));

        // 3. Seed Doctor Profiles
        Doctor doc1 = doctorRepository.save(new Doctor(
                docUser1, "DOC-1001", "Cardiologist", "MD, FACC, Cardiology", 12, "MED-LIC-8821", cardiology, BigDecimal.valueOf(600.00)
        ));

        Doctor doc2 = doctorRepository.save(new Doctor(
                docUser2, "DOC-1002", "Neurologist", "DM, Neurology, Harvard Med", 9, "MED-LIC-9932", neurology, BigDecimal.valueOf(750.00)
        ));

        Doctor doc3 = doctorRepository.save(new Doctor(
                docUser3, "DOC-1003", "Pediatrician", "MD, Pediatrics, Johns Hopkins", 7, "MED-LIC-7711", pediatrics, BigDecimal.valueOf(500.00)
        ));

        Doctor doc4 = doctorRepository.save(new Doctor(
                docUser4, "DOC-1004", "Orthopedic Surgeon", "MS, Orthopedics, Stanford", 15, "MED-LIC-6644", orthopedics, BigDecimal.valueOf(650.00)
        ));

        // 4. Seed Doctor Availabilities (Mon - Fri 09:00 - 17:00)
        List<Doctor> doctors = Arrays.asList(doc1, doc2, doc3, doc4);
        List<String> weekdays = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");

        for (Doctor doc : doctors) {
            for (String day : weekdays) {
                availabilityRepository.save(new DoctorAvailability(
                        doc, day, LocalTime.of(9, 0), LocalTime.of(17, 0), true
                ));
            }
        }

        // 5. Seed Patient Profiles
        Patient pat1 = patientRepository.save(new Patient(
                patUser1, "PAT-1001", LocalDate.of(1988, 5, 14), "MALE", "O+", "742 Evergreen Terrace, Springfield", "Jane Doe (Spouse)", "+1-555-321-4568"
        ));

        Patient pat2 = patientRepository.save(new Patient(
                patUser2, "PAT-1002", LocalDate.of(1993, 11, 22), "FEMALE", "A+", "124 Conch Street, Pacific City", "Mark Jenkins (Brother)", "+1-555-987-6544"
        ));

        Patient pat3 = patientRepository.save(new Patient(
                patUser3, "PAT-1003", LocalDate.of(1975, 2, 8), "MALE", "B+", "31 Elm Street, Boston", "Lisa Taylor (Daughter)", "+1-555-456-7891"
        ));

        // 6. Seed Appointments
        LocalDate today = LocalDate.now();

        // Appointment 1: Completed with consultation, prescription, bill, and payment
        Appointment app1 = appointmentRepository.save(new Appointment(
                pat1, doc1, today.minusDays(3), LocalTime.of(10, 0), "Regular cardiac rhythm checkup and mild chest flutter", "COMPLETED"
        ));

        // Appointment 2: Confirmed today
        Appointment app2 = appointmentRepository.save(new Appointment(
                pat2, doc2, today, LocalTime.of(11, 30), "Recurring migraine headaches and blurred vision episodes", "CONFIRMED"
        ));

        // Appointment 3: Pending for tomorrow
        Appointment app3 = appointmentRepository.save(new Appointment(
                pat3, doc3, today.plusDays(1), LocalTime.of(14, 0), "Seasonal allergic bronchitis and wheezing", "PENDING"
        ));

        // Appointment 4: Cancelled past
        Appointment app4 = appointmentRepository.save(new Appointment(
                pat1, doc4, today.minusDays(7), LocalTime.of(15, 30), "Knee joint stiffness consultation", "CANCELLED"
        ));

        // Appointment 5: Completed past with full record
        Appointment app5 = appointmentRepository.save(new Appointment(
                pat2, doc1, today.minusDays(14), LocalTime.of(9, 30), "Routine blood pressure spike management", "COMPLETED"
        ));

        // 7. Seed Consultations, Prescriptions & Medical Records
        Consultation consult1 = consultationRepository.save(new Consultation(
                app1, pat1, doc1,
                "Mild chest tightness following exertion, slight fatigue.",
                "Sinus Tachycardia with trace anxiety-induced arrhythmia.",
                "Prescribed beta-blocker, hydration increase, lifestyle adjustments and 30-day follow-up ECG.",
                "Patient responded well to stress reduction recommendations. Blood pressure 125/82 mmHg.",
                today.minusDays(3)
        ));

        medicalRecordRepository.save(new MedicalRecord(
                pat1, doc1, consult1, today.minusDays(3),
                "Sinus Tachycardia",
                "Metoprolol 25mg daily + Lifestyle modifications",
                "Follow up in 30 days. Maintain blood pressure log."
        ));

        Prescription presc1 = prescriptionRepository.save(new Prescription(
                consult1, pat1, doc1, today.minusDays(3), "Take after morning meal with plenty of water. Avoid caffeine."
        ));
        presc1.addItem(new PrescriptionItem("Metoprolol Tartrate", "25 mg", "Once daily", "30 days", "Take morning with food"));
        presc1.addItem(new PrescriptionItem("Coenzyme Q10", "100 mg", "Once daily", "60 days", "Dietary supplement for cardiac energy"));
        prescriptionRepository.save(presc1);

        // Consultation 2 for Appointment 5
        Consultation consult5 = consultationRepository.save(new Consultation(
                app5, pat2, doc1,
                "Elevated systolic BP, morning headaches.",
                "Stage 1 Essential Hypertension.",
                "Prescribed Telmisartan 40mg daily with sodium restriction.",
                "Target BP < 120/80 mmHg within 4 weeks.",
                today.minusDays(14)
        ));

        medicalRecordRepository.save(new MedicalRecord(
                pat2, doc1, consult5, today.minusDays(14),
                "Stage 1 Essential Hypertension",
                "Telmisartan 40mg once daily + Dietary sodium restriction",
                "Repeat lipid profile and electrolytes in 3 months."
        ));

        Prescription presc5 = prescriptionRepository.save(new Prescription(
                consult5, pat2, doc1, today.minusDays(14), "Monitor blood pressure daily in the morning."
        ));
        presc5.addItem(new PrescriptionItem("Telmisartan", "40 mg", "Once daily", "30 days", "Take in morning before food"));
        prescriptionRepository.save(presc5);

        // 8. Seed Bills & Payments
        Bill bill1 = billRepository.save(new Bill(
                pat1, app1, BigDecimal.valueOf(600.00), BigDecimal.valueOf(85.00), BigDecimal.valueOf(150.00), today.minusDays(3)
        ));
        bill1.setStatus("PAID");
        billRepository.save(bill1);

        paymentRepository.save(new Payment(
                bill1, BigDecimal.valueOf(835.00), "CARD", "TXN-88291039", "SUCCESS"
        ));

        Bill bill2 = billRepository.save(new Bill(
                pat2, app2, BigDecimal.valueOf(750.00), BigDecimal.ZERO, BigDecimal.valueOf(200.00), today
        ));
        bill2.setStatus("PENDING");
        billRepository.save(bill2);

        Bill bill3 = billRepository.save(new Bill(
                pat3, app3, BigDecimal.valueOf(500.00), BigDecimal.ZERO, BigDecimal.ZERO, today.plusDays(1)
        ));
        bill3.setStatus("PENDING");
        billRepository.save(bill3);

        Bill bill5 = billRepository.save(new Bill(
                pat2, app5, BigDecimal.valueOf(600.00), BigDecimal.valueOf(45.00), BigDecimal.ZERO, today.minusDays(14)
        ));
        bill5.setStatus("PAID");
        billRepository.save(bill5);

        paymentRepository.save(new Payment(
                bill5, BigDecimal.valueOf(645.00), "UPI", "UPI-77182903", "SUCCESS"
        ));

        // 9. Seed Notifications
        notificationRepository.save(new Notification(patUser1, "Appointment Booked", "Your appointment with Dr. Rahul Sharma has been completed.", "APPOINTMENT"));
        notificationRepository.save(new Notification(patUser1, "Prescription Created", "Dr. Rahul Sharma has issued a prescription for your recent consultation.", "PRESCRIPTION"));
        notificationRepository.save(new Notification(patUser1, "Payment Confirmed", "Payment of $835.00 received for Bill #" + bill1.getId(), "BILLING"));

        notificationRepository.save(new Notification(patUser2, "Appointment Reminder", "You have an appointment today with Dr. Priya Patel at 11:30 AM.", "APPOINTMENT"));
        notificationRepository.save(new Notification(docUser1, "New Appointment", "Patient John Doe had consultation completed.", "APPOINTMENT"));
        notificationRepository.save(new Notification(docUser2, "Schedule Update", "Upcoming consultation today at 11:30 AM with Sarah Jenkins.", "APPOINTMENT"));

        // 10. Seed Audit Logs
        auditLogRepository.save(new AuditLog("SYSTEM_INITIALIZATION", "system", "System", 1L, "ApexCare Hospital Management System initialized successfully"));
        auditLogRepository.save(new AuditLog("USER_REGISTRATION", "system", "User", admin.getId(), "System administrator account established"));
        auditLogRepository.save(new AuditLog("BOOK_APPOINTMENT", patUser1.getEmail(), "Appointment", app1.getId(), "Booked appointment with Dr. Rahul Sharma"));
        auditLogRepository.save(new AuditLog("COMPLETE_CONSULTATION", docUser1.getEmail(), "Consultation", consult1.getId(), "Conducted clinical consultation for John Doe"));
        auditLogRepository.save(new AuditLog("RECORD_PAYMENT", patUser1.getEmail(), "Payment", 1L, "Processed card payment of $835.00"));

        System.out.println(">>> ApexCare Database Seed Completed Successfully!");
    }
}
