package com.apexcare.repository;

import com.apexcare.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBillId(Long billId);
    List<Payment> findTop10ByOrderByPaymentDateDesc();

    @Query("SELECT p FROM Payment p WHERE p.bill.patient.id = :patientId ORDER BY p.paymentDate DESC")
    List<Payment> findByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal calculateTotalPaid();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.bill.id = :billId AND p.status = 'SUCCESS'")
    BigDecimal calculatePaidAmountForBill(@Param("billId") Long billId);
}
