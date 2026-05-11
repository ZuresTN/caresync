package com.caresync.controllers;

import com.caresync.dao.PatientDAO;
import com.caresync.models.Gender;
import com.caresync.models.Patient;
import com.caresync.services.AuditService;
import com.caresync.services.ValidationService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class PatientManagementController {
    @FXML private TextField searchField;
    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, String> nameColumn;
    @FXML private TableColumn<Patient, String> phoneColumn;
    @FXML private TableColumn<Patient, String> emailColumn;
    @FXML private TableColumn<Patient, String> bloodColumn;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private ChoiceBox<Gender> genderChoice;
    @FXML private DatePicker dobPicker;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextArea addressArea;
    @FXML private TextField bloodGroupField;
    @FXML private TextField allergiesField;
    @FXML private TextField emergencyField;
    @FXML private Label statusLabel;

    private final PatientDAO patientDAO = new PatientDAO();
    private final ValidationService validationService = new ValidationService();
    private final AuditService auditService = new AuditService();
    private Patient selectedPatient;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        bloodColumn.setCellValueFactory(new PropertyValueFactory<>("bloodGroup"));
        genderChoice.setItems(FXCollections.observableArrayList(Gender.values()));
        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, old, patient) -> selectPatient(patient));
        searchField.textProperty().addListener((obs, old, value) -> refresh());
        refresh();
    }

    @FXML
    private void savePatient() {
        try {
            validationService.requireText(firstNameField.getText(), "First name");
            validationService.requireText(lastNameField.getText(), "Last name");
            validationService.validateEmail(emailField.getText(), "Reminder email", true);
            validationService.validatePhone(phoneField.getText(), "Phone", false);
            Patient patient = selectedPatient == null ? new Patient() : selectedPatient;
            boolean creating = selectedPatient == null;
            patient.setFirstName(firstNameField.getText().trim());
            patient.setLastName(lastNameField.getText().trim());
            patient.setGender(genderChoice.getValue());
            patient.setDateOfBirth(dobPicker.getValue());
            patient.setPhone(phoneField.getText().trim());
            patient.setEmail(emailField.getText().trim());
            patient.setAddress(addressArea.getText().trim());
            patient.setBloodGroup(bloodGroupField.getText().trim());
            patient.setAllergies(allergiesField.getText().trim());
            patient.setEmergencyContact(emergencyField.getText().trim());
            int patientId = patientDAO.save(patient);
            auditService.record(creating ? "CREATE" : "UPDATE", "PATIENT", patientId, "Patient " + patient.getFullName().trim() + " saved.");
            clearForm();
            refresh();
            setStatus("Patient saved successfully.", true);
        } catch (Exception ex) {
            setStatus("Could not save patient: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void deletePatient() {
        if (selectedPatient == null) {
            return;
        }
        try {
            patientDAO.delete(selectedPatient.getId());
            auditService.record("DELETE", "PATIENT", selectedPatient.getId(), "Patient " + selectedPatient.getFullName().trim() + " removed.");
            clearForm();
            refresh();
            setStatus("Patient removed.", true);
        } catch (Exception ex) {
            setStatus("Could not remove patient: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void clearForm() {
        selectedPatient = null;
        firstNameField.clear();
        lastNameField.clear();
        genderChoice.setValue(null);
        dobPicker.setValue(null);
        phoneField.clear();
        emailField.clear();
        addressArea.clear();
        bloodGroupField.clear();
        allergiesField.clear();
        emergencyField.clear();
    }

    private void selectPatient(Patient patient) {
        selectedPatient = patient;
        if (patient == null) {
            return;
        }
        firstNameField.setText(patient.getFirstName());
        lastNameField.setText(patient.getLastName());
        genderChoice.setValue(patient.getGender());
        dobPicker.setValue(patient.getDateOfBirth());
        phoneField.setText(patient.getPhone());
        emailField.setText(patient.getEmail());
        addressArea.setText(patient.getAddress());
        bloodGroupField.setText(patient.getBloodGroup());
        allergiesField.setText(patient.getAllergies());
        emergencyField.setText(patient.getEmergencyContact());
    }

    private void refresh() {
        try {
            patientTable.setItems(FXCollections.observableArrayList(patientDAO.findAll(searchField.getText())));
        } catch (Exception ex) {
            setStatus("Could not load patients: " + ex.getMessage(), false);
        }
    }

    private void setStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-text", "error-text");
        statusLabel.getStyleClass().add(success ? "success-text" : "error-text");
    }
}
