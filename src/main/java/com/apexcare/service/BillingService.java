package com.apexcare.service;

import com.apexcare.dto.BillDTO;
import com.apexcare.dto.BillRequest;
import com.apexcare.dto.PaymentDTO;
import com.apexcare.entity.Appointment;
import com.apexcare.entity.Bill;
import com.apexcare.entity.Patient;
import com.apexcare.entity.Role;
import com.apexcare.exception.BadRequestException;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.AppointmentRepository;
import com.apexcare.repository.BillRepository;
import com.apexcare.repository.PatientRepository;
import com.apexcare.repository.PaymentRepository;
import com.apexcare.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BillingService {

    private final BillRepository billRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public BillingService(BillRepository billRepository,
                          PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          PaymentRepository paymentRepository,
                          NotificationService notificationService,
                          AuditLogService auditLogService) {
        this.billRepository = billRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<BillDTO> getAllBills(Long patientId, String status, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.PATIENT) {
            Patient p = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            patientId = p.getId();
        }

        return billRepository.filterBills(patientId, status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BillDTO getBillById(Long id, UserPrincipal currentUser) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + id));

        if (currentUser.getRole() == Role.PATIENT) {
            Patient p = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            if (!bill.getPatient().getId().equals(p.getId())) {
                throw new BadRequestException("You are only permitted to view your own bills");
            }
        }

        return mapToDTO(bill);
    }

    @Transactional(readOnly = true)
    public List<BillDTO> getBillsByPatientId(Long patientId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.PATIENT) {
            Patient p = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            if (!p.getId().equals(patientId)) {
                throw new BadRequestException("You are only permitted to view your own bills");
            }
        }
        return billRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BillDTO createBill(BillRequest request, String performedBy) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));

        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + request.getAppointmentId()));
        }

        LocalDate date = request.getBillDate() != null ? request.getBillDate() : LocalDate.now();

        Bill bill = new Bill(
                patient,
                appointment,
                request.getConsultationFee(),
                request.getMedicineCharges(),
                request.getOtherCharges(),
                date
        );

        Bill saved = billRepository.save(bill);

        notificationService.createNotification(
                patient.getUser().getId(),
                "New Bill Generated",
                "An invoice #" + saved.getId() + " for $" + saved.getTotalAmount() + " has been generated for your hospital account.",
                "BILLING"
        );

        auditLogService.log("GENERATE_BILL", performedBy, "Bill", saved.getId(),
                "Generated bill of $" + saved.getTotalAmount() + " for patient " + patient.getUser().getName());

        return mapToDTO(saved);
    }

    @Transactional
    public BillDTO updateBill(Long id, BillRequest request, String performedBy) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + id));

        bill.setConsultationFee(request.getConsultationFee());
        bill.setMedicineCharges(request.getMedicineCharges());
        bill.setOtherCharges(request.getOtherCharges());
        bill.recalculateTotal();

        BigDecimal paid = paymentRepository.calculatePaidAmountForBill(id);
        if (paid.compareTo(bill.getTotalAmount()) >= 0) {
            bill.setStatus("PAID");
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            bill.setStatus("PARTIALLY_PAID");
        } else {
            bill.setStatus("PENDING");
        }

        Bill updated = billRepository.save(bill);
        auditLogService.log("UPDATE_BILL", performedBy, "Bill", id, "Updated bill ID: " + id);
        return mapToDTO(updated);
    }

    public BillDTO mapToDTO(Bill bill) {
        BillDTO dto = new BillDTO();
        dto.setId(bill.getId());
        dto.setPatientId(bill.getPatient().getId());
        dto.setPatientName(bill.getPatient().getUser().getName());
        dto.setPatientCode(bill.getPatient().getPatientCode());
        if (bill.getAppointment() != null) {
            dto.setAppointmentId(bill.getAppointment().getId());
        }
        dto.setConsultationFee(bill.getConsultationFee());
        dto.setMedicineCharges(bill.getMedicineCharges());
        dto.setOtherCharges(bill.getOtherCharges());
        dto.setTotalAmount(bill.getTotalAmount());
        dto.setStatus(bill.getStatus());
        dto.setBillDate(bill.getBillDate());
        dto.setCreatedAt(bill.getCreatedAt());

        BigDecimal paid = paymentRepository.calculatePaidAmountForBill(bill.getId());
        dto.setPaidAmount(paid);
        BigDecimal balance = bill.getTotalAmount().subtract(paid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;
        dto.setBalanceAmount(balance);

        List<PaymentDTO> paymentDTOs = paymentRepository.findByBillId(bill.getId()).stream()
                .map(p -> new PaymentDTO(p.getId(), bill.getId(), bill.getPatient().getId(), bill.getPatient().getUser().getName(), p.getAmount(), p.getPaymentMethod(), p.getPaymentDate(), p.getTransactionReference(), p.getStatus()))
                .collect(Collectors.toList());
        dto.setPayments(paymentDTOs);

        return dto;
    }
}
