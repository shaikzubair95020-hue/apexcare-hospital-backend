package com.apexcare.service;

import com.apexcare.dto.DepartmentDTO;
import com.apexcare.dto.DepartmentRequest;
import com.apexcare.entity.Department;
import com.apexcare.exception.DuplicateResourceException;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.DepartmentRepository;
import com.apexcare.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final AuditLogService auditLogService;

    public DepartmentService(DepartmentRepository departmentRepository, DoctorRepository doctorRepository, AuditLogService auditLogService) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DepartmentDTO> getActiveDepartments() {
        return departmentRepository.findByStatus("ACTIVE").stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentDTO getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return mapToDTO(dept);
    }

    @Transactional
    public DepartmentDTO createDepartment(DepartmentRequest request, String performedBy) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Department with name '" + request.getName() + "' already exists");
        }
        Department dept = new Department(request.getName(), request.getDescription(), request.getStatus());
        Department saved = departmentRepository.save(dept);
        auditLogService.log("CREATE_DEPARTMENT", performedBy, "Department", saved.getId(), "Created department " + saved.getName());
        return mapToDTO(saved);
    }

    @Transactional
    public DepartmentDTO updateDepartment(Long id, DepartmentRequest request, String performedBy) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        if (!dept.getName().equalsIgnoreCase(request.getName()) && departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Department with name '" + request.getName() + "' already exists");
        }

        dept.setName(request.getName());
        dept.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            dept.setStatus(request.getStatus());
        }
        Department updated = departmentRepository.save(dept);
        auditLogService.log("UPDATE_DEPARTMENT", performedBy, "Department", updated.getId(), "Updated department " + updated.getName());
        return mapToDTO(updated);
    }

    @Transactional
    public void deleteDepartment(Long id, String performedBy) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        dept.setStatus("INACTIVE");
        departmentRepository.save(dept);
        auditLogService.log("DEACTIVATE_DEPARTMENT", performedBy, "Department", id, "Deactivated department " + dept.getName());
    }

    private DepartmentDTO mapToDTO(Department dept) {
        long docCount = doctorRepository.findByDepartmentId(dept.getId()).size();
        return new DepartmentDTO(dept.getId(), dept.getName(), dept.getDescription(), dept.getStatus(), docCount);
    }
}
