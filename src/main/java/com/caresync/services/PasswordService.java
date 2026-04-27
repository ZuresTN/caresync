package com.caresync.services;

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;

public class PasswordService {
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public boolean matches(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder("CS-");
        for (int i = 0; i < 12; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }
}
