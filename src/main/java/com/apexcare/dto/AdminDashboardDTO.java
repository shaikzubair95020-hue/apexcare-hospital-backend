package com.apexcare.dto;

import com.apexcare.entity.AuditLog;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdminDashboardDTO {
    private long totalPatients;
    private long totalDoctors;
    private long totalDepartments;
    private long todayAppointments;
    private long pendingAppointments;
    private long completedAppointments;
    private long cancelledAppointments;
    private BigDecimal totalRevenue;
    private BigDecimal pendingRevenue;
    private List<Map<String, Object>> monthlyRevenue;
    private List<Map<String, Object>> appointmentStats;
    private List<Map<String, Object>> departmentStats;
    private List<AppointmentDTO> recentAppointments;
    private List<AuditLog> recentActivities;

    public AdminDashboardDTO() {}

    public long getTotalPatients() { return totalPatients; }
    public void setTotalPatients(long totalPatients) { this.totalPatients = totalPatients; }

    public long getTotalDoctors() { return totalDoctors; }
    public void setTotalDoctors(long totalDoctors) { this.totalDoctors = totalDoctors; }

    public long getTotalDepartments() { return totalDepartments; }
    public void setTotalDepartments(long totalDepartments) { this.totalDepartments = totalDepartments; }

    public long getTodayAppointments() { return todayAppointments; }
    public void setTodayAppointments(long todayAppointments) { this.todayAppointments = todayAppointments; }

    public long getPendingAppointments() { return pendingAppointments; }
    public void setPendingAppointments(long pendingAppointments) { this.pendingAppointments = pendingAppointments; }

    public long getCompletedAppointments() { return completedAppointments; }
    public void setCompletedAppointments(long completedAppointments) { this.completedAppointments = completedAppointments; }

    public long getCancelledAppointments() { return cancelledAppointments; }
    public void setCancelledAppointments(long cancelledAppointments) { this.cancelledAppointments = cancelledAppointments; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getPendingRevenue() { return pendingRevenue; }
    public void setPendingRevenue(BigDecimal pendingRevenue) { this.pendingRevenue = pendingRevenue; }

    public List<Map<String, Object>> getMonthlyRevenue() { return monthlyRevenue; }
    public void setMonthlyRevenue(List<Map<String, Object>> monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }

    public List<Map<String, Object>> getAppointmentStats() { return appointmentStats; }
    public void setAppointmentStats(List<Map<String, Object>> appointmentStats) { this.appointmentStats = appointmentStats; }

    public List<Map<String, Object>> getDepartmentStats() { return departmentStats; }
    public void setDepartmentStats(List<Map<String, Object>> departmentStats) { this.departmentStats = departmentStats; }

    public List<AppointmentDTO> getRecentAppointments() { return recentAppointments; }
    public void setRecentAppointments(List<AppointmentDTO> recentAppointments) { this.recentAppointments = recentAppointments; }

    public List<AuditLog> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<AuditLog> recentActivities) { this.recentActivities = recentActivities; }
}
