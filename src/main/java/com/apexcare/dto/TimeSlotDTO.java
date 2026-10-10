package com.apexcare.dto;

import java.time.LocalTime;

public class TimeSlotDTO {
    private LocalTime time;
    private String formattedTime;
    private boolean available;

    public TimeSlotDTO() {}

    public TimeSlotDTO(LocalTime time, String formattedTime, boolean available) {
        this.time = time;
        this.formattedTime = formattedTime;
        this.available = available;
    }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
