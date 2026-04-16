package com.caresync.services;

import com.caresync.models.Appointment;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class GoogleCalendarService {
    private static final DateTimeFormatter GOOGLE_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public URI buildAppointmentUri(Appointment appointment) {
        if (appointment.getAppointmentDate() == null || appointment.getAppointmentTime() == null) {
            throw new IllegalArgumentException("Appointment date and time are required.");
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime localStart = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getAppointmentTime());
        LocalDateTime localEnd = localStart.plusMinutes(30);
        String startUtc = localStart.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).format(GOOGLE_DATE_TIME);
        String endUtc = localEnd.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).format(GOOGLE_DATE_TIME);

        String title = "CareSync appointment"
                + valueOrEmpty(" - ", appointment.getPatientName())
                + valueOrEmpty(" with Dr. ", appointment.getDoctorName());
        String details = """
                Patient: %s
                Doctor: %s
                Reason: %s
                Notes: %s
                """.formatted(
                nullToEmpty(appointment.getPatientName()),
                nullToEmpty(appointment.getDoctorName()),
                nullToEmpty(appointment.getReason()),
                nullToEmpty(appointment.getNotes())
        ).trim();

        String url = "https://calendar.google.com/calendar/r/eventedit"
                + "?action=TEMPLATE"
                + "&dates=" + encode(startUtc + "/" + endUtc)
                + "&stz=" + encode(zone.getId())
                + "&etz=" + encode(zone.getId())
                + "&text=" + encode(title)
                + "&details=" + encode(details)
                + "&location=" + encode("CareSync Clinic");
        return URI.create(url);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String valueOrEmpty(String prefix, String value) {
        return value == null || value.isBlank() ? "" : prefix + value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
