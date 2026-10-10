package com.apexcare.service;

import com.apexcare.dto.DoctorAvailabilityDTO;
import com.apexcare.dto.TimeSlotDTO;
import com.apexcare.entity.Doctor;
import com.apexcare.entity.DoctorAvailability;
import com.apexcare.exception.ResourceNotFoundException;
import com.apexcare.repository.AppointmentRepository;
import com.apexcare.repository.DoctorAvailabilityRepository;
import com.apexcare.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DoctorAvailabilityService {

    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorAvailabilityService(DoctorAvailabilityRepository availabilityRepository,
                                     DoctorRepository doctorRepository,
                                     AppointmentRepository appointmentRepository) {
        this.availabilityRepository = availabilityRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DoctorAvailabilityDTO> getAvailabilityByDoctor(Long doctorId) {
        return availabilityRepository.findByDoctorId(doctorId).stream()
                .map(a -> new DoctorAvailabilityDTO(a.getId(), doctorId, a.getDayOfWeek(), a.getStartTime(), a.getEndTime(), a.isAvailable()))
                .collect(Collectors.toList());
    }

    @Transactional
    public DoctorAvailabilityDTO saveOrUpdateAvailability(Long doctorId, String dayOfWeek, LocalTime startTime, LocalTime endTime, boolean available) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        Optional<DoctorAvailability> existing = availabilityRepository.findByDoctorIdAndDayOfWeekIgnoreCase(doctorId, dayOfWeek.toUpperCase());
        DoctorAvailability availability;
        if (existing.isPresent()) {
            availability = existing.get();
            availability.setStartTime(startTime);
            availability.setEndTime(endTime);
            availability.setAvailable(available);
        } else {
            availability = new DoctorAvailability(doctor, dayOfWeek.toUpperCase(), startTime, endTime, available);
        }

        DoctorAvailability saved = availabilityRepository.save(availability);
        return new DoctorAvailabilityDTO(saved.getId(), doctorId, saved.getDayOfWeek(), saved.getStartTime(), saved.getEndTime(), saved.isAvailable());
    }

    @Transactional
    public void deleteAvailability(Long id) {
        if (!availabilityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Availability record not found with ID: " + id);
        }
        availabilityRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TimeSlotDTO> getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        if (!"ACTIVE".equalsIgnoreCase(doctor.getStatus())) {
            return new ArrayList<>();
        }

        String dayOfWeek = date.getDayOfWeek().name();
        Optional<DoctorAvailability> optAvail = availabilityRepository.findByDoctorIdAndDayOfWeekIgnoreCase(doctorId, dayOfWeek);

        if (optAvail.isEmpty() || !optAvail.get().isAvailable()) {
            return new ArrayList<>();
        }

        DoctorAvailability avail = optAvail.get();
        List<TimeSlotDTO> slots = new ArrayList<>();

        LocalTime current = avail.getStartTime();
        LocalTime end = avail.getEndTime();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        while (current.isBefore(end)) {
            // Check if slot has an active (non-cancelled) appointment
            boolean isBooked = appointmentRepository.existsActiveAppointment(doctorId, date, current);

            // Also if date is today, check if current slot time has already passed
            boolean isPast = date.isEqual(LocalDate.now()) && current.isBefore(LocalTime.now());

            boolean isSlotAvailable = !isBooked && !isPast;

            slots.add(new TimeSlotDTO(current, current.format(timeFormatter), isSlotAvailable));
            current = current.plusMinutes(30);
        }

        return slots;
    }
}
