package com.caresync.services;

import com.caresync.dao.UserDAO;
import com.caresync.models.Role;
import com.caresync.models.User;
import com.caresync.utils.SessionManager;

import java.util.Optional;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    private final PasswordService passwordService = new PasswordService();
    private final ValidationService validationService = new ValidationService();

    public Optional<User> login(String username, String password) throws java.sql.SQLException {
        Optional<User> user = userDAO.findByUsername(username);
        if (user.isPresent()
                && user.get().isActive()
                && passwordService.matches(password, user.get().getPasswordHash())) {
            SessionManager.setCurrentUser(user.get());
            return user;
        }
        return Optional.empty();
    }

    public void logout() {
        SessionManager.clear();
    }

    public boolean changePassword(User user, String currentPassword, String newPassword) throws java.sql.SQLException {
        if (user == null || !passwordService.matches(currentPassword, user.getPasswordHash())) {
            return false;
        }
        validationService.validatePassword(newPassword);
        String hash = passwordService.hash(newPassword);
        userDAO.updatePassword(user.getId(), hash);
        user.setPasswordHash(hash);
        user.setPasswordMustChange(false);
        SessionManager.setCurrentUser(user);
        return true;
    }

    public String resetPasswordByAdmin(User user) throws java.sql.SQLException {
        if (user == null || user.getId() <= 0) {
            throw new IllegalArgumentException("Select a valid user.");
        }
        String temporaryPassword = passwordService.generateTemporaryPassword();
        userDAO.setTemporaryPassword(user.getId(), passwordService.hash(temporaryPassword));
        user.setPasswordMustChange(true);
        return temporaryPassword;
    }

    public void ensureInitialAdmin() throws java.sql.SQLException {
        if (userDAO.countAll() > 0) {
            return;
        }
        String password = System.getenv("CARESYNC_INITIAL_ADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("No users exist. Set CARESYNC_INITIAL_ADMIN_PASSWORD, then restart.");
        }
        validationService.validatePassword(password);

        User admin = new User();
        admin.setFullName(defaultValue(System.getenv("CARESYNC_INITIAL_ADMIN_NAME"), "Clinic Administrator"));
        admin.setUsername(defaultValue(System.getenv("CARESYNC_INITIAL_ADMIN_USERNAME"), "admin"));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin.setPasswordMustChange(true);
        admin.setPasswordHash(passwordService.hash(password));
        userDAO.save(admin);
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
