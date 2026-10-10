package com.apexcare.service;

import com.apexcare.dto.PaymentDTO;
import com.apexcare.dto.PaymentRequest;
import com.apexcare.entity.Bill;
import com.apexcare.entity.Patient;
import com.apexcare.entity.Payment;
import com.apexcare.entity.Role;
import com.apexcare.exception.BadRequestException;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.BillRepository;
import com.apexcare.repository.PatientRepository;
import com.apexcare.repository.PaymentRepository;
import com.apexcare.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public PaymentService(PaymentRepository paymentRepository,
                          BillRepository billRepository,
                          PatientRepository patientRepository,
                          NotificationService notificationService,
                          AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.billRepository = billRepository;
        this.patientRepository = patientRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PaymentDTO recordPayment(PaymentRequest request, String performedBy) {
        Bill bill = billRepository.findById(request.getBillId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + request.getBillId()));

        if ("CANCELLED".equalsIgnoreCase(bill.getStatus())) {
            throw new BadRequestException("Cannot make payments toward a cancelled bill");
        }

        BigDecimal alreadyPaid = paymentRepository.calculatePaidAmountForBill(bill.getId());
        BigDecimal outstanding = bill.getTotalAmount().subtract(alreadyPaid);
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) outstanding = BigDecimal.ZERO;

        // Business Rule: Payment cannot exceed outstanding bill amount
        if (request.getAmount().compareTo(outstanding) > 0) {
            throw new BadRequestException("Payment amount of $" + request.getAmount() +
                    " exceeds outstanding balance of $" + outstanding);
        }

        String txnRef = request.getTransactionReference();
        if (txnRef == null || txnRef.isBlank()) {
            txnRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        Payment payment = new Payment(
                bill,
                request.getAmount(),
                request.getPaymentMethod(),
                txnRef,
                "SUCCESS"
        );
        Payment saved = paymentRepository.save(payment);

        // Update bill status
        BigDecimal newTotalPaid = alreadyPaid.add(request.getAmount());
        if (newTotalPaid.compareTo(bill.getTotalAmount()) >= 0) {
            bill.setStatus("PAID");
        } else {
            bill.setStatus("PARTIALLY_PAID");
        }
        billRepository.save(bill);

        // Notification
        notificationService.createNotification(
                bill.getPatient().getUser().getId(),
                "Payment Confirmed",
                "Your payment of $" + saved.getAmount() + " via " + saved.getPaymentMethod() + " for bill #" + bill.getId() + " was processed successfully. Ref: " + saved.getTransactionReference(),
                "BILLING"
        );

        auditLogService.log("RECORD_PAYMENT", performedBy, "Payment", saved.getId(),
                "Recorded payment of $" + saved.getAmount() + " for bill ID: " + bill.getId());

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> getPaymentsByBillId(Long billId) {
        return paymentRepository.findByBillId(billId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> getPaymentsByPatientId(Long patientId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.PATIENT) {
            Patient p = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
            if (!p.getId().equals(patientId)) {
                throw new BadRequestException("You are only permitted to view your own payments");
            }
        }
        return paymentRepository.findByPatientId(patientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentDTO getPaymentById(Long id) {
        Payment p = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + id));
        return mapToDTO(p);
    }

    public PaymentDTO mapToDTO(Payment p) {
        return new PaymentDTO(
                p.getId(),
                p.getBill().getId(),
                p.getBill().getPatient().getId(),
                p.getBill().getPatient().getUser().getName(),
                p.getAmount(),
                p.getPaymentMethod(),
                p.getPaymentDate(),
                p.getTransactionReference(),
                p.getStatus()
        );
    }
}
