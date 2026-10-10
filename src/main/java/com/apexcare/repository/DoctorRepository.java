package com.apexcare.repository;

import com.apexcare.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    Optional<Doctor> findByDoctorCode(String doctorCode);
    List<Doctor> findByDepartmentId(Long departmentId);
    List<Doctor> findByStatus(String status);
    long countByStatus(String status);

    @Query("SELECT d FROM Doctor d WHERE " +
           "(:deptId IS NULL OR d.department.id = :deptId) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:query IS NULL OR (" +
           "LOWER(d.user.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.doctorCode) LIKE LOWER(CONCAT('%', :query, '%'))))")
    List<Doctor> searchDoctors(@Param("deptId") Long deptId, @Param("status") String status, @Param("query") String query);
}
