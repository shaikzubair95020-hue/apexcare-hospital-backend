package com.apexcare.dto;

import jakarta.validation.constraints.NotBlank;

public class DepartmentDTO {
    private Long id;
    private String name;
    private String description;
    private String status;
    private long doctorCount;

    public DepartmentDTO() {}

    public DepartmentDTO(Long id, String name, String description, String status, long doctorCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.doctorCount = doctorCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDoctorCount() { return doctorCount; }
    public void setDoctorCount(long doctorCount) { this.doctorCount = doctorCount; }
}
