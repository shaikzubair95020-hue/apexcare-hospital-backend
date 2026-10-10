package com.apexcare.dto;

import java.util.List;

public class DoctorDashboardDTO {
    private Long doctorId;
    private String doctorName;
    private String doctorCode;
    private String specialization;
    private long todayAppointmentsCount;
    private long upcomingAppointmentsCount;
    private long completedConsultationsCount;
    private long pendingConsultationsCount;
    private long totalPatientsHandled;
    private List<AppointmentDTO> todayAppointments;
    private List<AppointmentDTO> upcomingAppointments;
    private List<PatientDTO> recentPatients;
    private List<NotificationDTO> notifications;

    public DoctorDashboardDTO() {}

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorCode() { return doctorCode; }
    public void setDoctorCode(String doctorCode) { this.doctorCode = doctorCode; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public long getTodayAppointmentsCount() { return todayAppointmentsCount; }
    public void setTodayAppointmentsCount(long todayAppointmentsCount) { this.todayAppointmentsCount = todayAppointmentsCount; }

    public long getUpcomingAppointmentsCount() { return upcomingAppointmentsCount; }
    public void setUpcomingAppointmentsCount(long upcomingAppointmentsCount) { this.upcomingAppointmentsCount = upcomingAppointmentsCount; }

    public long getCompletedConsultationsCount() { return completedConsultationsCount; }
    public void setCompletedConsultationsCount(long completedConsultationsCount) { this.completedConsultationsCount = completedConsultationsCount; }

    public long getPendingConsultationsCount() { return pendingConsultationsCount; }
    public void setPendingConsultationsCount(long pendingConsultationsCount) { this.pendingConsultationsCount = pendingConsultationsCount; }

    public long getTotalPatientsHandled() { return totalPatientsHandled; }
    public void setTotalPatientsHandled(long totalPatientsHandled) { this.totalPatientsHandled = totalPatientsHandled; }

    public List<AppointmentDTO> getTodayAppointments() { return todayAppointments; }
    public void setTodayAppointments(List<AppointmentDTO> todayAppointments) { this.todayAppointments = todayAppointments; }

    public List<AppointmentDTO> getUpcomingAppointments() { return upcomingAppointments; }
    public void setUpcomingAppointments(List<AppointmentDTO> upcomingAppointments) { this.upcomingAppointments = upcomingAppointments; }

    public List<PatientDTO> getRecentPatients() { return recentPatients; }
    public void setRecentPatients(List<PatientDTO> recentPatients) { this.recentPatients = recentPatients; }

    public List<NotificationDTO> getNotifications() { return notifications; }
    public void setNotifications(List<NotificationDTO> notifications) { this.notifications = notifications; }
}
