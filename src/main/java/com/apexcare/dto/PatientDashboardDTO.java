package com.apexcare.dto;

import java.math.BigDecimal;
import java.util.List;

public class PatientDashboardDTO {
    private Long patientId;
    private String patientName;
    private String patientCode;
    private AppointmentDTO upcomingAppointment;
    private long totalAppointments;
    private long totalPrescriptions;
    private long totalMedicalRecords;
    private long pendingBillsCount;
    private BigDecimal pendingBillsAmount;
    private List<AppointmentDTO> recentAppointments;
    private List<PrescriptionDTO> recentPrescriptions;
    private List<BillDTO> recentBills;
    private List<NotificationDTO> notifications;

    public PatientDashboardDTO() {}

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientCode() { return patientCode; }
    public void setPatientCode(String patientCode) { this.patientCode = patientCode; }

    public AppointmentDTO getUpcomingAppointment() { return upcomingAppointment; }
    public void setUpcomingAppointment(AppointmentDTO upcomingAppointment) { this.upcomingAppointment = upcomingAppointment; }

    public long getTotalAppointments() { return totalAppointments; }
    public void setTotalAppointments(long totalAppointments) { this.totalAppointments = totalAppointments; }

    public long getTotalPrescriptions() { return totalPrescriptions; }
    public void setTotalPrescriptions(long totalPrescriptions) { this.totalPrescriptions = totalPrescriptions; }

    public long getTotalMedicalRecords() { return totalMedicalRecords; }
    public void setTotalMedicalRecords(long totalMedicalRecords) { this.totalMedicalRecords = totalMedicalRecords; }

    public long getPendingBillsCount() { return pendingBillsCount; }
    public void setPendingBillsCount(long pendingBillsCount) { this.pendingBillsCount = pendingBillsCount; }

    public BigDecimal getPendingBillsAmount() { return pendingBillsAmount; }
    public void setPendingBillsAmount(BigDecimal pendingBillsAmount) { this.pendingBillsAmount = pendingBillsAmount; }

    public List<AppointmentDTO> getRecentAppointments() { return recentAppointments; }
    public void setRecentAppointments(List<AppointmentDTO> recentAppointments) { this.recentAppointments = recentAppointments; }

    public List<PrescriptionDTO> getRecentPrescriptions() { return recentPrescriptions; }
    public void setRecentPrescriptions(List<PrescriptionDTO> recentPrescriptions) { this.recentPrescriptions = recentPrescriptions; }

    public List<BillDTO> getRecentBills() { return recentBills; }
    public void setRecentBills(List<BillDTO> recentBills) { this.recentBills = recentBills; }

    public List<NotificationDTO> getNotifications() { return notifications; }
    public void setNotifications(List<NotificationDTO> notifications) { this.notifications = notifications; }
}
