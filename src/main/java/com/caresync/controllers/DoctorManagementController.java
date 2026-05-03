package com.caresync.controllers;

import com.caresync.dao.DoctorDAO;
import com.caresync.dao.UserDAO;
import com.caresync.models.Doctor;
import com.caresync.models.Role;
import com.caresync.models.User;
import com.caresync.services.PasswordService;
import com.caresync.services.ValidationService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class DoctorManagementController {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private TableView<Doctor> doctorTable;
    @FXML private TableColumn<Doctor, String> nameColumn;
    @FXML private TableColumn<Doctor, String> specialtyColumn;
    @FXML private TableColumn<Doctor, String> phoneColumn;
    @FXML private TableColumn<Doctor, String> availabilityColumn;
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField specializationField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField roomField;
    @FXML private TextField availabilityField;
    @FXML private CheckBox mondayCheck;
    @FXML private CheckBox tuesdayCheck;
    @FXML private CheckBox wednesdayCheck;
    @FXML private CheckBox thursdayCheck;
    @FXML private CheckBox fridayCheck;
    @FXML private CheckBox saturdayCheck;
    @FXML private CheckBox sundayCheck;
    @FXML private TextField availabilityStartField;
    @FXML private TextField availabilityEndField;
    @FXML private Label statusLabel;

    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final UserDAO userDAO = new UserDAO();
    private final PasswordService passwordService = new PasswordService();
    private final ValidationService validationService = new ValidationService();
    private Doctor selectedDoctor;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        specialtyColumn.setCellValueFactory(new PropertyValueFactory<>("specialization"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        availabilityColumn.setCellValueFactory(new PropertyValueFactory<>("availability"));
        doctorTable.getSelectionModel().selectedItemProperty().addListener((obs, old, doctor) -> selectDoctor(doctor));
        setDefaultAvailabilityBuilder();
        refresh();
    }

    @FXML
    private void applyAvailabilityBuilder() {
        try {
            availabilityField.setText(buildAvailability());
            setStatus("Availability applied.", true);
        } catch (IllegalArgumentException ex) {
            setStatus(ex.getMessage(), false);
        }
    }

    @FXML
    private void saveDoctor() {
        try {
            if (availabilityField.getText().isBlank() && hasSelectedAvailabilityDay()) {
                availabilityField.setText(buildAvailability());
            }
            validationService.requireText(fullNameField.getText(), "Doctor name");
            validationService.requireText(usernameField.getText(), "Username");
            validationService.validateEmail(emailField.getText(), "Email", false);
            validationService.validatePhone(phoneField.getText(), "Phone", false);
            if (selectedDoctor == null || !passwordField.getText().isBlank()) {
                validationService.validatePassword(passwordField.getText());
            }
            User user = selectedDoctor == null ? new User() : userDAO.findAll().stream()
                    .filter(existing -> existing.getId() == selectedDoctor.getUserId())
                    .findFirst()
                    .orElseGet(User::new);
            user.setFullName(fullNameField.getText().trim());
            user.setUsername(usernameField.getText().trim());
            user.setRole(Role.DOCTOR);
            user.setActive(true);
            if (user.getId() == 0 || !passwordField.getText().isBlank()) {
                user.setPasswordHash(passwordService.hash(passwordField.getText()));
                user.setPasswordMustChange(user.getId() == 0);
            }
            int userId = userDAO.save(user);

            Doctor doctor = selectedDoctor == null ? new Doctor() : selectedDoctor;
            doctor.setUserId(userId);
            doctor.setSpecialization(defaultText(specializationField.getText(), "General Medicine"));
            doctor.setPhone(phoneField.getText().trim());
            doctor.setEmail(emailField.getText().trim());
            doctor.setRoom(roomField.getText().trim());
            doctor.setAvailability(defaultText(availabilityField.getText(), "Mon-Fri 9 AM - 4 PM"));
            doctorDAO.save(doctor);

            clearForm();
            refresh();
            setStatus("Doctor saved successfully.", true);
        } catch (Exception ex) {
            setStatus("Could not save doctor: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void clearForm() {
        selectedDoctor = null;
        fullNameField.clear();
        usernameField.clear();
        passwordField.clear();
        specializationField.clear();
        phoneField.clear();
        emailField.clear();
        roomField.clear();
        availabilityField.clear();
        setDefaultAvailabilityBuilder();
    }

    private void selectDoctor(Doctor doctor) {
        selectedDoctor = doctor;
        if (doctor == null) {
            return;
        }
        fullNameField.setText(doctor.getFullName());
        specializationField.setText(doctor.getSpecialization());
        phoneField.setText(doctor.getPhone());
        emailField.setText(doctor.getEmail());
        roomField.setText(doctor.getRoom());
        availabilityField.setText(doctor.getAvailability());
        applyAvailabilityToBuilder(doctor.getAvailability());
        passwordField.clear();
        try {
            userDAO.findAll().stream()
                    .filter(user -> user.getId() == doctor.getUserId())
                    .findFirst()
                    .ifPresent(user -> usernameField.setText(user.getUsername()));
        } catch (Exception ex) {
            setStatus("Could not load doctor login details.", false);
        }
    }

    private void refresh() {
        try {
            doctorTable.setItems(FXCollections.observableArrayList(doctorDAO.findAll()));
        } catch (Exception ex) {
            setStatus("Could not load doctors: " + ex.getMessage(), false);
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String buildAvailability() {
        String days = selectedDays();
        if (days.isBlank()) {
            throw new IllegalArgumentException("Select at least one availability day.");
        }

        LocalTime start = parseAvailabilityTime(availabilityStartField.getText(), "Start time");
        LocalTime end = parseAvailabilityTime(availabilityEndField.getText(), "End time");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        return days + ", " + start.format(TIME_FORMATTER) + "-" + end.format(TIME_FORMATTER);
    }

    private LocalTime parseAvailabilityTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value == null ? "" : value.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " must use HH:mm, for example 09:00.");
        }
    }

    private String selectedDays() {
        StringBuilder builder = new StringBuilder();
        appendDay(builder, mondayCheck, "Mon");
        appendDay(builder, tuesdayCheck, "Tue");
        appendDay(builder, wednesdayCheck, "Wed");
        appendDay(builder, thursdayCheck, "Thu");
        appendDay(builder, fridayCheck, "Fri");
        appendDay(builder, saturdayCheck, "Sat");
        appendDay(builder, sundayCheck, "Sun");
        return builder.toString();
    }

    private void appendDay(StringBuilder builder, CheckBox checkBox, String day) {
        if (checkBox.isSelected()) {
            if (!builder.isEmpty()) {
                builder.append("-");
            }
            builder.append(day);
        }
    }

    private boolean hasSelectedAvailabilityDay() {
        return mondayCheck.isSelected()
                || tuesdayCheck.isSelected()
                || wednesdayCheck.isSelected()
                || thursdayCheck.isSelected()
                || fridayCheck.isSelected()
                || saturdayCheck.isSelected()
                || sundayCheck.isSelected();
    }

    private void setDefaultAvailabilityBuilder() {
        mondayCheck.setSelected(true);
        tuesdayCheck.setSelected(false);
        wednesdayCheck.setSelected(true);
        thursdayCheck.setSelected(false);
        fridayCheck.setSelected(true);
        saturdayCheck.setSelected(false);
        sundayCheck.setSelected(false);
        availabilityStartField.setText("09:00");
        availabilityEndField.setText("15:00");
    }

    private void applyAvailabilityToBuilder(String availability) {
        setDefaultAvailabilityBuilder();
        if (availability == null || availability.isBlank()) {
            return;
        }
        String[] parts = availability.split(",", 2);
        if (parts.length < 2) {
            return;
        }
        if (parts.length > 0) {
            clearDayChecks();
            for (String day : parts[0].split("-")) {
                selectDay(day.trim());
            }
        }
        String[] times = parts[1].trim().split("-", 2);
        if (times.length == 2) {
            availabilityStartField.setText(times[0].trim());
            availabilityEndField.setText(times[1].trim());
        }
    }

    private void clearDayChecks() {
        mondayCheck.setSelected(false);
        tuesdayCheck.setSelected(false);
        wednesdayCheck.setSelected(false);
        thursdayCheck.setSelected(false);
        fridayCheck.setSelected(false);
        saturdayCheck.setSelected(false);
        sundayCheck.setSelected(false);
    }

    private void selectDay(String day) {
        switch (day.toLowerCase(Locale.ROOT)) {
            case "mon" -> mondayCheck.setSelected(true);
            case "tue" -> tuesdayCheck.setSelected(true);
            case "wed" -> wednesdayCheck.setSelected(true);
            case "thu" -> thursdayCheck.setSelected(true);
            case "fri" -> fridayCheck.setSelected(true);
            case "sat" -> saturdayCheck.setSelected(true);
            case "sun" -> sundayCheck.setSelected(true);
            default -> {
            }
        }
    }

    private void setStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-text", "error-text");
        statusLabel.getStyleClass().add(success ? "success-text" : "error-text");
    }
}
