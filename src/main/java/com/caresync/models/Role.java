package com.caresync.models;

public enum Role {
    ADMIN("Admin"),
    DOCTOR("Doctor"),
    RECEPTIONIST("Receptionist"),
    PATIENT("Patient");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
