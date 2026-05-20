package com.caresync.services;

import com.caresync.dao.AppointmentDAO;
import com.caresync.models.Appointment;
import com.caresync.models.AppointmentStatus;
import com.caresync.models.Role;

import java.time.LocalDate;

public class AppointmentValidationService {
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    public void validateForSave(Appointment appointment, Role actorRole) throws java.sql.SQLException {
        if (appointment.getPatientId() <= 0) {
            throw new IllegalArgumentException("Patient is required.");
        }
        if (appointment.getDoctorId() <= 0) {
            throw new IllegalArgumentException("Doctor is required.");
        }
        if (appointment.getAppointmentDate() == null || appointment.getAppointmentTime() == null) {
            throw new IllegalArgumentException("Appointment date and time are required.");
        }
        if (appointment.getStatus() == null) {
            appointment.setStatus(AppointmentStatus.SCHEDULED);
        }
        if (actorRole == Role.PATIENT) {
            throw new IllegalArgumentException("Patients cannot manage appointments from this screen.");
        }
        if (appointment.getStatus() != AppointmentStatus.CANCELLED
                && appointment.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("New or active appointments cannot be scheduled in the past.");
        }
        if (appointmentDAO.hasDoctorConflict(
                appointment.getId(),
                appointment.getDoctorId(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime())) {
            throw new IllegalArgumentException("That doctor already has an appointment at this time.");
        }
    }
}
