package com.apexcare.controller;

import com.apexcare.dto.DepartmentDTO;
import com.apexcare.dto.DepartmentRequest;
import com.apexcare.security.UserPrincipal;
import com.apexcare.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments(@RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(departmentService.getActiveDepartments());
        }
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDTO> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDTO> createDepartment(@Valid @RequestBody DepartmentRequest request,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(departmentService.createDepartment(request, userPrincipal.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDTO> updateDepartment(@PathVariable Long id,
                                                          @Valid @RequestBody DepartmentRequest request,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request, userPrincipal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal userPrincipal) {
        departmentService.deleteDepartment(id, userPrincipal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
