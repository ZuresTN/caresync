package com.caresync.controllers;

import com.caresync.Main;
import com.caresync.models.User;
import com.caresync.services.AuthService;
import com.caresync.services.ToastService;
import com.caresync.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.StackPane;

public class ChangePasswordController {
    @FXML private StackPane root;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void changePassword() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            setStatus("Session expired. Sign in again.", false);
            return;
        }
        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
            setStatus("New passwords do not match.", false);
            return;
        }
        try {
            if (!authService.changePassword(user, currentPasswordField.getText(), newPasswordField.getText())) {
                setStatus("Current password is incorrect.", false);
                return;
            }
            Main.setRoot("dashboard_shell", 1280, 820);
        } catch (Exception ex) {
            setStatus(ex.getMessage(), false);
        }
    }

    @FXML
    private void logout() {
        try {
            authService.logout();
            Main.setRoot("login", 1080, 720);
        } catch (Exception ex) {
            setStatus("Could not return to login.", false);
        }
    }

    private void setStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-text", "error-text");
        statusLabel.getStyleClass().add(success ? "success-text" : "error-text");
        if (root != null) {
            ToastService.show(root, message, success);
        }
    }
}
