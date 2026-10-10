package com.apexcare.repository;

import com.apexcare.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    Optional<Bill> findByAppointmentId(Long appointmentId);
    List<Bill> findByStatus(String status);
    List<Bill> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.status = 'PAID'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.status = 'PENDING'")
    BigDecimal calculatePendingRevenue();

    @Query("SELECT b FROM Bill b WHERE " +
           "(:patientId IS NULL OR b.patient.id = :patientId) AND " +
           "(:status IS NULL OR b.status = :status) " +
           "ORDER BY b.createdAt DESC")
    List<Bill> filterBills(@Param("patientId") Long patientId, @Param("status") String status);
}
