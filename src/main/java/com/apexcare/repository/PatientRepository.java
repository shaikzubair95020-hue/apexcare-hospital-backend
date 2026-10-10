package com.apexcare.repository;

import com.apexcare.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserId(Long userId);
    Optional<Patient> findByUserEmail(String email);
    Optional<Patient> findByPatientCode(String patientCode);
    List<Patient> findByStatus(String status);
    long countByStatus(String status);

    @Query("SELECT p FROM Patient p WHERE " +
           "(:query IS NULL OR " +
           "LOWER(p.user.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.patientCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.user.phone) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.user.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Patient> searchPatients(@Param("query") String query);
}
