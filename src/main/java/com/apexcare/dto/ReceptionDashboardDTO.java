package com.apexcare.dto;

import java.math.BigDecimal;
import java.util.List;

public class ReceptionDashboardDTO {
    private long todayAppointments;
    private long registeredPatients;
    private long pendingAppointments;
    private long completedAppointments;
    private long pendingBills;
    private BigDecimal todayRevenue;
    private List<AppointmentDTO> todayAppointmentList;
    private List<PatientDTO> recentPatients;

    public ReceptionDashboardDTO() {}

    public long getTodayAppointments() { return todayAppointments; }
    public void setTodayAppointments(long todayAppointments) { this.todayAppointments = todayAppointments; }

    public long getRegisteredPatients() { return registeredPatients; }
    public void setRegisteredPatients(long registeredPatients) { this.registeredPatients = registeredPatients; }

    public long getPendingAppointments() { return pendingAppointments; }
    public void setPendingAppointments(long pendingAppointments) { this.pendingAppointments = pendingAppointments; }

    public long getCompletedAppointments() { return completedAppointments; }
    public void setCompletedAppointments(long completedAppointments) { this.completedAppointments = completedAppointments; }

    public long getPendingBills() { return pendingBills; }
    public void setPendingBills(long pendingBills) { this.pendingBills = pendingBills; }

    public BigDecimal getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }

    public List<AppointmentDTO> getTodayAppointmentList() { return todayAppointmentList; }
    public void setTodayAppointmentList(List<AppointmentDTO> todayAppointmentList) { this.todayAppointmentList = todayAppointmentList; }

    public List<PatientDTO> getRecentPatients() { return recentPatients; }
    public void setRecentPatients(List<PatientDTO> recentPatients) { this.recentPatients = recentPatients; }
}
