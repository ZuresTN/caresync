package com.caresync.controllers;

import com.caresync.dao.ReceptionistDAO;
import com.caresync.dao.UserDAO;
import com.caresync.models.Receptionist;
import com.caresync.models.Role;
import com.caresync.models.User;
import com.caresync.services.PasswordService;
import com.caresync.services.ValidationService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class ReceptionistManagementController {
    @FXML private TableView<Receptionist> receptionistTable;
    @FXML private TableColumn<Receptionist, String> nameColumn;
    @FXML private TableColumn<Receptionist, String> phoneColumn;
    @FXML private TableColumn<Receptionist, String> emailColumn;
    @FXML private TableColumn<Receptionist, String> shiftColumn;
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField shiftField;
    @FXML private Label statusLabel;

    private final ReceptionistDAO receptionistDAO = new ReceptionistDAO();
    private final UserDAO userDAO = new UserDAO();
    private final PasswordService passwordService = new PasswordService();
    private final ValidationService validationService = new ValidationService();
    private Receptionist selectedReceptionist;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        shiftColumn.setCellValueFactory(new PropertyValueFactory<>("shift"));
        receptionistTable.getSelectionModel().selectedItemProperty().addListener((obs, old, receptionist) -> selectReceptionist(receptionist));
        refresh();
    }

    @FXML
    private void saveReceptionist() {
        try {
            validationService.requireText(fullNameField.getText(), "Receptionist name");
            validationService.requireText(usernameField.getText(), "Username");
            validationService.validateEmail(emailField.getText(), "Email", false);
            validationService.validatePhone(phoneField.getText(), "Phone", false);
            if (selectedReceptionist == null || !passwordField.getText().isBlank()) {
                validationService.validatePassword(passwordField.getText());
            }
            User user = selectedReceptionist == null ? new User() : userDAO.findAll().stream()
                    .filter(existing -> existing.getId() == selectedReceptionist.getUserId())
                    .findFirst()
                    .orElseGet(User::new);
            user.setFullName(fullNameField.getText().trim());
            user.setUsername(usernameField.getText().trim());
            user.setRole(Role.RECEPTIONIST);
            user.setActive(true);
            if (user.getId() == 0 || !passwordField.getText().isBlank()) {
                user.setPasswordHash(passwordService.hash(passwordField.getText()));
                user.setPasswordMustChange(user.getId() == 0);
            }
            int userId = userDAO.save(user);

            Receptionist receptionist = selectedReceptionist == null ? new Receptionist() : selectedReceptionist;
            receptionist.setUserId(userId);
            receptionist.setPhone(phoneField.getText().trim());
            receptionist.setEmail(emailField.getText().trim());
            receptionist.setShift(defaultText(shiftField.getText(), "Morning"));
            receptionistDAO.save(receptionist);

            clearForm();
            refresh();
            setStatus("Receptionist saved successfully.", true);
        } catch (Exception ex) {
            setStatus("Could not save receptionist: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void clearForm() {
        selectedReceptionist = null;
        fullNameField.clear();
        usernameField.clear();
        passwordField.clear();
        phoneField.clear();
        emailField.clear();
        shiftField.clear();
    }

    private void selectReceptionist(Receptionist receptionist) {
        selectedReceptionist = receptionist;
        if (receptionist == null) {
            return;
        }
        fullNameField.setText(receptionist.getFullName());
        phoneField.setText(receptionist.getPhone());
        emailField.setText(receptionist.getEmail());
        shiftField.setText(receptionist.getShift());
        passwordField.clear();
        try {
            userDAO.findAll().stream()
                    .filter(user -> user.getId() == receptionist.getUserId())
                    .findFirst()
                    .ifPresent(user -> usernameField.setText(user.getUsername()));
        } catch (Exception ex) {
            setStatus("Could not load login details.", false);
        }
    }

    private void refresh() {
        try {
            receptionistTable.setItems(FXCollections.observableArrayList(receptionistDAO.findAll()));
        } catch (Exception ex) {
            setStatus("Could not load receptionists: " + ex.getMessage(), false);
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void setStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-text", "error-text");
        statusLabel.getStyleClass().add(success ? "success-text" : "error-text");
    }
}
