package com.apexcare.dto;

import jakarta.validation.constraints.NotBlank;

public class DepartmentRequest {

    @NotBlank(message = "Department name is required")
    private String name;

    private String description;
    private String status = "ACTIVE";

    public DepartmentRequest() {}

    public DepartmentRequest(String name, String description, String status) {
        this.name = name;
        this.description = description;
        this.status = status != null ? status : "ACTIVE";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
