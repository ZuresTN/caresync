package com.caresync.controllers;

import com.caresync.Main;
import com.caresync.models.Role;
import com.caresync.models.User;
import com.caresync.services.AuthService;
import com.caresync.utils.AnimationUtil;
import com.caresync.utils.SessionManager;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class DashboardShellController {
    @FXML private StackPane contentPane;
    @FXML private Label userNameLabel;
    @FXML private Label roleLabel;
    @FXML private Button dashboardButton;
    @FXML private Button usersButton;
    @FXML private Button doctorsButton;
    @FXML private Button receptionistsButton;
    @FXML private Button patientsButton;
    @FXML private Button appointmentsButton;
    @FXML private Button medicalButton;
    @FXML private Button auditButton;
    @FXML private Button settingsButton;
    @FXML private Button logoutButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }
        userNameLabel.setText(user.getFullName());
        roleLabel.setText(user.getRole().getLabel());
        configureRole(user.getRole());
        Platform.runLater(() -> {
            AnimationUtil.staggerIn(AnimationUtil.visibleNavNodes(
                    dashboardButton, usersButton, doctorsButton, patientsButton,
                    receptionistsButton, appointmentsButton, medicalButton, auditButton, settingsButton, logoutButton
            ));
            if (contentPane.getScene() != null) {
                AnimationUtil.animateInteractiveControls(contentPane.getScene().getRoot());
            }
        });
        openDashboard();
    }

    @FXML
    private void openDashboard() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }
        if (user.getRole() == Role.ADMIN) {
            loadPage("admin_dashboard");
        } else if (user.getRole() == Role.DOCTOR) {
            loadPage("doctor_dashboard");
        } else if (user.getRole() == Role.PATIENT) {
            loadPage("patient_dashboard");
        } else {
            loadPage("receptionist_dashboard");
        }
        markActive(dashboardButton);
    }

    @FXML private void openUsers() { loadPage("user_management"); markActive(usersButton); }
    @FXML private void openDoctors() { loadPage("doctor_management"); markActive(doctorsButton); }
    @FXML private void openReceptionists() { loadPage("receptionist_management"); markActive(receptionistsButton); }
    @FXML private void openPatients() { loadPage("patient_management"); markActive(patientsButton); }
    @FXML private void openAppointments() { loadPage("appointment_management"); markActive(appointmentsButton); }
    @FXML private void openMedical() { loadPage("medical_record"); markActive(medicalButton); }
    @FXML private void openAudit() { loadPage("audit_logs"); markActive(auditButton); }
    @FXML private void openSettings() { loadPage("settings"); markActive(settingsButton); }

    @FXML
    private void logout() {
        try {
            authService.logout();
            Main.setRoot("login", 1080, 720);
        } catch (Exception ex) {
            Label error = new Label("Could not log out: " + ex.getMessage());
            error.getStyleClass().add("error-text");
            contentPane.getChildren().setAll(error);
        }
    }

    private void configureRole(Role role) {
        setVisible(usersButton, role == Role.ADMIN);
        setVisible(doctorsButton, role == Role.ADMIN);
        setVisible(receptionistsButton, role == Role.ADMIN);
        setVisible(settingsButton, role == Role.ADMIN);
        setVisible(patientsButton, role == Role.ADMIN || role == Role.RECEPTIONIST);
        setVisible(appointmentsButton, role == Role.ADMIN || role == Role.RECEPTIONIST || role == Role.DOCTOR);
        setVisible(medicalButton, role == Role.ADMIN || role == Role.DOCTOR);
        setVisible(auditButton, role == Role.ADMIN);
    }

    private void loadPage(String fxml) {
        try {
            Node page = Main.loadFXML(fxml);
            if (contentPane.getChildren().isEmpty()) {
                showPage(page);
            } else {
                Node current = contentPane.getChildren().get(0);
                ParallelTransition exit = AnimationUtil.pageExit(current);
                exit.setOnFinished(event -> showPage(page));
                exit.play();
            }
        } catch (Exception ex) {
            Label error = new Label("Could not load page: " + ex.getMessage());
            error.getStyleClass().add("error-text");
            contentPane.getChildren().setAll(error);
        }
    }

    private void showPage(Node page) {
        contentPane.getChildren().setAll(page);
        ParallelTransition enter = AnimationUtil.pageEnter(page);
        enter.setOnFinished(event -> {
            if (page instanceof Parent parent) {
                AnimationUtil.revealPageChildren(parent);
                AnimationUtil.animateInteractiveControls(parent);
            }
        });
        enter.play();
    }

    private void markActive(Button selected) {
        for (Button button : new Button[]{dashboardButton, usersButton, doctorsButton, receptionistsButton, patientsButton, appointmentsButton, medicalButton, auditButton, settingsButton}) {
            button.getStyleClass().remove("nav-button-active");
        }
        selected.getStyleClass().add("nav-button-active");
        ScaleTransition pulse = new ScaleTransition(Duration.millis(120), selected);
        pulse.setToX(1.03);
        pulse.setToY(1.03);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }

    private void setVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
