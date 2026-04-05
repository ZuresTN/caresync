package com.caresync.utils;

public final class ClinicalContext {
    private static Integer selectedPatientId;
    private static Integer selectedAppointmentId;

    private ClinicalContext() {
    }

    public static void selectAppointment(int patientId, int appointmentId) {
        selectedPatientId = patientId;
        selectedAppointmentId = appointmentId;
    }

    public static Integer getSelectedPatientId() {
        return selectedPatientId;
    }

    public static Integer getSelectedAppointmentId() {
        return selectedAppointmentId;
    }

    public static void clear() {
        selectedPatientId = null;
        selectedAppointmentId = null;
    }
}
