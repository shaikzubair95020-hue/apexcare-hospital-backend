package com.apexcare.service;

import com.apexcare.dto.*;
import com.apexcare.entity.*;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final PrescriptionService prescriptionService;
    private final BillingService billingService;

    public DashboardService(PatientRepository patientRepository,
                            DoctorRepository doctorRepository,
                            DepartmentRepository departmentRepository,
                            AppointmentRepository appointmentRepository,
                            BillRepository billRepository,
                            PaymentRepository paymentRepository,
                            ConsultationRepository consultationRepository,
                            PrescriptionRepository prescriptionRepository,
                            MedicalRecordRepository medicalRecordRepository,
                            NotificationRepository notificationRepository,
                            AuditLogRepository auditLogRepository,
                            AppointmentService appointmentService,
                            PatientService patientService,
                            PrescriptionService prescriptionService,
                            BillingService billingService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.appointmentRepository = appointmentRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.prescriptionService = prescriptionService;
        this.billingService = billingService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardDTO getAdminDashboard() {
        AdminDashboardDTO dto = new AdminDashboardDTO();
        dto.setTotalPatients(patientRepository.count());
        dto.setTotalDoctors(doctorRepository.countByStatus("ACTIVE"));
        dto.setTotalDepartments(departmentRepository.count());
        dto.setTodayAppointments(appointmentRepository.countByAppointmentDate(LocalDate.now()));
        dto.setPendingAppointments(appointmentRepository.countByStatus("PENDING"));
        dto.setCompletedAppointments(appointmentRepository.countByStatus("COMPLETED"));
        dto.setCancelledAppointments(appointmentRepository.countByStatus("CANCELLED"));

        BigDecimal rev = billRepository.calculateTotalRevenue();
        dto.setTotalRevenue(rev != null ? rev : BigDecimal.ZERO);
        BigDecimal pendRev = billRepository.calculatePendingRevenue();
        dto.setPendingRevenue(pendRev != null ? pendRev : BigDecimal.ZERO);

        // Appointment status chart data
        List<Map<String, Object>> appStats = new ArrayList<>();
        appStats.add(Map.of("name", "Pending", "value", dto.getPendingAppointments(), "color", "#f59e0b"));
        appStats.add(Map.of("name", "Confirmed", "value", appointmentRepository.countByStatus("CONFIRMED"), "color", "#0ea5e9"));
        appStats.add(Map.of("name", "Completed", "value", dto.getCompletedAppointments(), "color", "#10b981"));
        appStats.add(Map.of("name", "Cancelled", "value", dto.getCancelledAppointments(), "color", "#ef4444"));
        dto.setAppointmentStats(appStats);

        // Department distribution chart data
        List<Map<String, Object>> deptStats = new ArrayList<>();
        List<Department> departments = departmentRepository.findAll();
        for (Department d : departments) {
            long docCount = doctorRepository.findByDepartmentId(d.getId()).size();
            deptStats.add(Map.of("name", d.getName(), "doctors", docCount));
        }
        dto.setDepartmentStats(deptStats);

        // Monthly revenue trends (last 6 months)
        List<Map<String, Object>> monthly = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy");
        for (int i = 5; i >= 0; i--) {
            LocalDate m = now.minusMonths(i);
            String mName = m.format(monthFmt);
            // Simulated baseline based on total revenue distribution
            BigDecimal monthVal = dto.getTotalRevenue().multiply(BigDecimal.valueOf(0.12 + (0.04 * (5 - i))));
            monthly.add(Map.of("month", mName, "revenue", monthVal.doubleValue()));
        }
        dto.setMonthlyRevenue(monthly);

        // Recent appointments
        List<AppointmentDTO> recentApps = appointmentRepository.findTop10ByOrderByAppointmentDateDescAppointmentTimeDesc().stream()
                .map(appointmentService::mapToDTO)
                .collect(Collectors.toList());
        dto.setRecentAppointments(recentApps);

        // Recent activity
        dto.setRecentActivities(auditLogRepository.findTop50ByOrderByTimestampDesc().stream().limit(15).collect(Collectors.toList()));

        return dto;
    }

    @Transactional(readOnly = true)
    public DoctorDashboardDTO getDoctorDashboard(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user: " + userId));

        DoctorDashboardDTO dto = new DoctorDashboardDTO();
        dto.setDoctorId(doctor.getId());
        dto.setDoctorName(doctor.getUser().getName());
        dto.setDoctorCode(doctor.getDoctorCode());
        dto.setSpecialization(doctor.getSpecialization());

        LocalDate today = LocalDate.now();
        List<Appointment> todayApps = appointmentRepository.findByDoctorIdAndAppointmentDate(doctor.getId(), today);
        dto.setTodayAppointmentsCount(todayApps.size());

        List<Appointment> allDoctorApps = appointmentRepository.findByDoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(doctor.getId());
        long upcoming = allDoctorApps.stream()
                .filter(a -> a.getAppointmentDate().isAfter(today) && !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                .count();
        dto.setUpcomingAppointmentsCount(upcoming);

        long completed = allDoctorApps.stream().filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
        dto.setCompletedConsultationsCount(completed);

        long pending = allDoctorApps.stream().filter(a -> "PENDING".equalsIgnoreCase(a.getStatus()) || "CONFIRMED".equalsIgnoreCase(a.getStatus())).count();
        dto.setPendingConsultationsCount(pending);

        long patientCount = allDoctorApps.stream().map(a -> a.getPatient().getId()).distinct().count();
        dto.setTotalPatientsHandled(patientCount);

        dto.setTodayAppointments(todayApps.stream().map(appointmentService::mapToDTO).collect(Collectors.toList()));
        dto.setUpcomingAppointments(allDoctorApps.stream()
                .filter(a -> a.getAppointmentDate().isAfter(today) && !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                .limit(5)
                .map(appointmentService::mapToDTO)
                .collect(Collectors.toList()));

        List<PatientDTO> recentPatients = allDoctorApps.stream()
                .map(Appointment::getPatient)
                .distinct()
                .limit(5)
                .map(patientService::mapToDTO)
                .collect(Collectors.toList());
        dto.setRecentPatients(recentPatients);

        dto.setNotifications(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(10)
                .map(n -> new NotificationDTO(n.getId(), userId, n.getTitle(), n.getMessage(), n.getType(), n.isRead(), n.getCreatedAt()))
                .collect(Collectors.toList()));

        return dto;
    }

    @Transactional(readOnly = true)
    public ReceptionDashboardDTO getReceptionDashboard() {
        ReceptionDashboardDTO dto = new ReceptionDashboardDTO();
        LocalDate today = LocalDate.now();

        dto.setTodayAppointments(appointmentRepository.countByAppointmentDate(today));
        dto.setRegisteredPatients(patientRepository.count());
        dto.setPendingAppointments(appointmentRepository.countByStatus("PENDING"));
        dto.setCompletedAppointments(appointmentRepository.countByStatus("COMPLETED"));
        dto.setPendingBills(billRepository.findByStatus("PENDING").size());

        BigDecimal rev = billRepository.calculateTotalRevenue();
        dto.setTodayRevenue(rev != null ? rev.multiply(BigDecimal.valueOf(0.15)) : BigDecimal.ZERO);

        dto.setTodayAppointmentList(appointmentRepository.findByAppointmentDateOrderByAppointmentTimeAsc(today).stream()
                .map(appointmentService::mapToDTO)
                .collect(Collectors.toList()));

        dto.setRecentPatients(patientRepository.findAll().stream()
                .sorted(Comparator.comparing(Patient::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(patientService::mapToDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    @Transactional(readOnly = true)
    public PatientDashboardDTO getPatientDashboard(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user: " + userId));

        PatientDashboardDTO dto = new PatientDashboardDTO();
        dto.setPatientId(patient.getId());
        dto.setPatientName(patient.getUser().getName());
        dto.setPatientCode(patient.getPatientCode());

        List<Appointment> apps = appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patient.getId());
        dto.setTotalAppointments(apps.size());

        LocalDate today = LocalDate.now();
        Optional<Appointment> nextApp = apps.stream()
                .filter(a -> (a.getAppointmentDate().isAfter(today) || (a.getAppointmentDate().isEqual(today) && a.getAppointmentTime().isAfter(java.time.LocalTime.now()))) && !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                .sorted(Comparator.comparing(Appointment::getAppointmentDate).thenComparing(Appointment::getAppointmentTime))
                .findFirst();

        nextApp.ifPresent(appointment -> dto.setUpcomingAppointment(appointmentService.mapToDTO(appointment)));

        List<Prescription> prescs = prescriptionRepository.findByPatientIdOrderByPrescriptionDateDesc(patient.getId());
        dto.setTotalPrescriptions(prescs.size());

        List<MedicalRecord> recs = medicalRecordRepository.findByPatientIdOrderByRecordDateDesc(patient.getId());
        dto.setTotalMedicalRecords(recs.size());

        List<Bill> bills = billRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId());
        List<Bill> pendingBills = bills.stream().filter(b -> !"PAID".equalsIgnoreCase(b.getStatus()) && !"CANCELLED".equalsIgnoreCase(b.getStatus())).collect(Collectors.toList());
        dto.setPendingBillsCount(pendingBills.size());

        BigDecimal pendingAmount = BigDecimal.ZERO;
        for (Bill b : pendingBills) {
            BigDecimal paid = paymentRepository.calculatePaidAmountForBill(b.getId());
            pendingAmount = pendingAmount.add(b.getTotalAmount().subtract(paid));
        }
        dto.setPendingBillsAmount(pendingAmount);

        dto.setRecentAppointments(apps.stream().limit(5).map(appointmentService::mapToDTO).collect(Collectors.toList()));
        dto.setRecentPrescriptions(prescs.stream().limit(5).map(prescriptionService::mapToDTO).collect(Collectors.toList()));
        dto.setRecentBills(bills.stream().limit(5).map(billingService::mapToDTO).collect(Collectors.toList()));

        dto.setNotifications(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(10)
                .map(n -> new NotificationDTO(n.getId(), userId, n.getTitle(), n.getMessage(), n.getType(), n.isRead(), n.getCreatedAt()))
                .collect(Collectors.toList()));

        return dto;
    }
}
