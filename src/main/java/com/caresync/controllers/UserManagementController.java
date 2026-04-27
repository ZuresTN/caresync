package com.caresync.controllers;

import com.caresync.dao.DoctorDAO;
import com.caresync.dao.ReceptionistDAO;
import com.caresync.dao.UserDAO;
import com.caresync.models.Doctor;
import com.caresync.models.Receptionist;
import com.caresync.models.Role;
import com.caresync.models.User;
import com.caresync.services.AuditService;
import com.caresync.services.AuthService;
import com.caresync.services.PasswordService;
import com.caresync.services.ValidationService;
import com.caresync.utils.SessionManager;
import com.caresync.utils.UiStyleUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class UserManagementController {
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ChoiceBox<Role> roleChoice;
    @FXML private CheckBox activeCheck;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField specialtyOrShiftField;
    @FXML private TextField roomField;
    @FXML private TextField availabilityField;
    @FXML private Label formTitleLabel;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final ReceptionistDAO receptionistDAO = new ReceptionistDAO();
    private final PasswordService passwordService = new PasswordService();
    private final ValidationService validationService = new ValidationService();
    private final AuthService authService = new AuthService();
    private final AuditService auditService = new AuditService();
    private User selectedUser;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("roleLabel"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusLabel"));
        UiStyleUtil.applyTextBadgeCell(roleColumn, "role");
        UiStyleUtil.applyTextBadgeCell(statusColumn, "account");
        roleChoice.setItems(FXCollections.observableArrayList(List.of(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST)));
        roleChoice.setValue(Role.RECEPTIONIST);
        activeCheck.setSelected(true);
        roleChoice.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> updateProfileHints());
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, user) -> selectUser(user));
        refresh();
        updateProfileHints();
    }

    @FXML
    private void saveUser() {
        try {
            validationService.requireText(fullNameField.getText(), "Full name");
            validationService.requireText(usernameField.getText(), "Username");
            validationService.validateEmail(emailField.getText(), "Email", false);
            validationService.validatePhone(phoneField.getText(), "Phone", false);
            if (selectedUser == null || !passwordField.getText().isBlank()) {
                validationService.validatePassword(passwordField.getText());
            }
            User user = selectedUser == null ? new User() : selectedUser;
            boolean creating = selectedUser == null;
            Role previousRole = creating ? null : selectedUser.getRole();
            user.setFullName(fullNameField.getText().trim());
            user.setUsername(usernameField.getText().trim());
            user.setRole(roleChoice.getValue());
            if (user.getRole() == null || user.getRole() == Role.PATIENT) {
                throw new IllegalArgumentException("Patients do not have login accounts. Add patients in Patient Management and use their email for reminders.");
            }
            user.setActive(activeCheck.isSelected());
            if (selectedUser == null || !passwordField.getText().isBlank()) {
                user.setPasswordHash(passwordService.hash(passwordField.getText()));
                user.setPasswordMustChange(selectedUser == null);
            }
            validateRoleChange(user.getId(), previousRole, user.getRole());
            int userId = userDAO.save(user);

            if (user.getRole() == Role.DOCTOR) {
                Doctor doctor = doctorDAO.findByUserId(userId).orElseGet(Doctor::new);
                doctor.setUserId(userId);
                doctor.setSpecialization(defaultText(specialtyOrShiftField.getText(), "General Medicine"));
                doctor.setPhone(phoneField.getText().trim());
                doctor.setEmail(emailField.getText().trim());
                doctor.setRoom(roomField.getText().trim());
                doctor.setAvailability(availabilityField.getText().trim());
                doctorDAO.save(doctor);
            } else if (user.getRole() == Role.RECEPTIONIST) {
                Receptionist receptionist = receptionistDAO.findByUserId(userId).orElseGet(Receptionist::new);
                receptionist.setUserId(userId);
                receptionist.setPhone(phoneField.getText().trim());
                receptionist.setEmail(emailField.getText().trim());
                receptionist.setShift(defaultText(specialtyOrShiftField.getText(), "Morning"));
                receptionistDAO.save(receptionist);
            }
            removeProfilesForOtherRoles(userId, previousRole, user.getRole());
            clearForm();
            refresh();
            auditService.record(creating ? "CREATE" : "UPDATE", "USER", userId, "User " + user.getUsername() + " saved.");
            setStatus("User saved successfully.", true);
        } catch (Exception ex) {
            setStatus("Could not save user: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void resetSelectedPassword() {
        if (selectedUser == null) {
            setStatus("Select a user first.", false);
            return;
        }
        try {
            int userId = selectedUser.getId();
            String username = selectedUser.getUsername();
            String temporaryPassword = authService.resetPasswordByAdmin(selectedUser);
            refresh();
            reselectUser(userId);
            setStatus("Temporary password for " + username + ": " + temporaryPassword, true);
        } catch (Exception ex) {
            setStatus("Could not reset password: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void deleteUser() {
        if (selectedUser == null) {
            return;
        }
        try {
            if (SessionManager.getCurrentUser() != null
                    && selectedUser.getId() == SessionManager.getCurrentUser().getId()) {
                throw new IllegalArgumentException("You cannot delete the account you are currently using.");
            }
            if (selectedUser.getRole() == Role.ADMIN && userDAO.countByRole(Role.ADMIN) <= 1) {
                throw new IllegalArgumentException("You cannot delete the last admin account.");
            }
            userDAO.delete(selectedUser.getId());
            auditService.record("DELETE", "USER", selectedUser.getId(), "User " + selectedUser.getUsername() + " removed.");
            clearForm();
            refresh();
            setStatus("User removed.", true);
        } catch (Exception ex) {
            setStatus("Could not remove user: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void clearForm() {
        selectedUser = null;
        formTitleLabel.setText("Create User");
        fullNameField.clear();
        usernameField.clear();
        passwordField.clear();
        roleChoice.setValue(Role.RECEPTIONIST);
        activeCheck.setSelected(true);
        phoneField.clear();
        emailField.clear();
        specialtyOrShiftField.clear();
        roomField.clear();
        availabilityField.clear();
    }

    private void selectUser(User user) {
        selectedUser = user;
        if (user == null) {
            return;
        }
        formTitleLabel.setText("Edit User");
        fullNameField.setText(user.getFullName());
        usernameField.setText(user.getUsername());
        passwordField.clear();
        roleChoice.setValue(user.getRole());
        activeCheck.setSelected(user.isActive());
        phoneField.clear();
        emailField.clear();
        specialtyOrShiftField.clear();
        roomField.clear();
        availabilityField.clear();
        try {
            if (user.getRole() == Role.DOCTOR) {
                doctorDAO.findByUserId(user.getId()).ifPresent(doctor -> {
                    phoneField.setText(doctor.getPhone());
                    emailField.setText(doctor.getEmail());
                    specialtyOrShiftField.setText(doctor.getSpecialization());
                    roomField.setText(doctor.getRoom());
                    availabilityField.setText(doctor.getAvailability());
                });
            } else if (user.getRole() == Role.RECEPTIONIST) {
                receptionistDAO.findByUserId(user.getId()).ifPresent(receptionist -> {
                    phoneField.setText(receptionist.getPhone());
                    emailField.setText(receptionist.getEmail());
                    specialtyOrShiftField.setText(receptionist.getShift());
                });
            }
        } catch (Exception ex) {
            setStatus("Could not load profile details.", false);
        }
    }

    private void refresh() {
        try {
            userTable.setItems(FXCollections.observableArrayList(
                    userDAO.findAll().stream()
                            .filter(user -> user.getRole() != Role.PATIENT)
                            .toList()
            ));
        } catch (Exception ex) {
            setStatus("Could not load users: " + ex.getMessage(), false);
        }
    }

    private void reselectUser(int userId) {
        userTable.getItems().stream()
                .filter(user -> user.getId() == userId)
                .findFirst()
                .ifPresent(user -> userTable.getSelectionModel().select(user));
    }

    private void updateProfileHints() {
        Role role = roleChoice.getValue();
        if (role == Role.DOCTOR) {
            specialtyOrShiftField.setPromptText("Specialization");
            roomField.setDisable(false);
            availabilityField.setDisable(false);
        } else if (role == Role.RECEPTIONIST) {
            specialtyOrShiftField.setPromptText("Shift");
            roomField.setDisable(true);
            availabilityField.setDisable(true);
        } else {
            specialtyOrShiftField.setPromptText("Optional profile detail");
            roomField.setDisable(true);
            availabilityField.setDisable(true);
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void removeProfilesForOtherRoles(int userId, Role previousRole, Role currentRole) throws java.sql.SQLException {
        if (currentRole != Role.DOCTOR && doctorDAO.findByUserId(userId).isPresent()) {
            doctorDAO.deleteByUserId(userId);
        }
        if (currentRole != Role.RECEPTIONIST && receptionistDAO.findByUserId(userId).isPresent()) {
            receptionistDAO.deleteByUserId(userId);
        }
    }

    private void validateRoleChange(int userId, Role previousRole, Role currentRole) throws java.sql.SQLException {
        if (userId <= 0 || previousRole == null || previousRole == currentRole) {
            return;
        }
        if (previousRole == Role.DOCTOR && currentRole != Role.DOCTOR && doctorDAO.hasClinicalDataForUser(userId)) {
            throw new IllegalArgumentException("This doctor has appointments or medical history. Keep the doctor role or move the clinical data first.");
        }
    }

    private void setStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-text", "error-text");
        statusLabel.getStyleClass().add(success ? "success-text" : "error-text");
    }
}
