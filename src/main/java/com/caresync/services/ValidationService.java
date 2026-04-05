package com.caresync.services;

import java.util.regex.Pattern;

public class ValidationService {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s.]{7,40}$");

    public void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }

    public void validateEmail(String value, String label, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new IllegalArgumentException(label + " is required.");
            }
            return;
        }
        if (!EMAIL.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(label + " must be a valid email address.");
        }
    }

    public void validatePhone(String value, String label, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new IllegalArgumentException(label + " is required.");
            }
            return;
        }
        if (!PHONE.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(label + " must be a valid phone number.");
        }
    }

    public void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("Password must include at least one letter and one number.");
        }
    }
}
