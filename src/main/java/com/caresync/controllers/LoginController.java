package com.caresync.controllers;

import com.caresync.Main;
import com.caresync.models.User;
import com.caresync.services.AuthService;
import com.caresync.services.DatabaseMigrationService;
import com.caresync.services.ToastService;
import com.caresync.utils.AnimationUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Optional;

public class LoginController {
    @FXML private StackPane root;
    @FXML private Node loginPanel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;

    private final AuthService authService = new AuthService();
    private final DatabaseMigrationService databaseMigrationService = new DatabaseMigrationService();

    @FXML
    private void initialize() {
        try {
            databaseMigrationService.ensureCurrentSchema();
            authService.ensureInitialAdmin();
        } catch (Exception ex) {
            showError("Database setup needs attention: " + ex.getMessage());
        }
        Platform.runLater(() -> {
            AnimationUtil.pageEnter(loginPanel).play();
            AnimationUtil.animateInteractiveControls(root);
        });
    }

    @FXML
    private void handleLogin() {
        messageLabel.setText("");
        loginButton.setDisable(true);
        try {
            Optional<User> user = authService.login(usernameField.getText().trim(), passwordField.getText());
            if (user.isPresent()) {
                if (user.get().isPasswordMustChange()) {
                    Main.setRoot("change_password", 720, 520);
                    return;
                }
                root.getChildren().setAll(Main.loadFXML("loading"));
                PauseTransition delay = new PauseTransition(Duration.seconds(1.2));
                delay.setOnFinished(event -> {
                    try {
                        Main.setRoot("dashboard_shell", 1280, 820);
                    } catch (Exception ex) {
                        showError("Could not open dashboard: " + ex.getMessage());
                    }
                });
                delay.play();
            } else {
                showError("Invalid username, password, or inactive account.");
                loginButton.setDisable(false);
            }
        } catch (Exception ex) {
            showError("Login failed: " + ex.getMessage());
            loginButton.setDisable(false);
        }
    }

    @FXML
    private void handleResetPassword() {
        messageLabel.getStyleClass().removeAll("error-text", "success-text");
        messageLabel.getStyleClass().add("success-text");
        messageLabel.setText("Ask an administrator to reset your password from Users and Access Control.");
    }

    private void showError(String message) {
        messageLabel.getStyleClass().removeAll("success-text", "error-text");
        messageLabel.getStyleClass().add("error-text");
        messageLabel.setText(message);
        if (root != null) {
            ToastService.show(root, message, false);
        }
    }
}
