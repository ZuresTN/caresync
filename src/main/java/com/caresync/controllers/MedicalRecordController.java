package com.caresync.controllers;

import com.caresync.dao.DoctorDAO;
import com.caresync.dao.MedicalHistoryDAO;
import com.caresync.dao.PatientDAO;
import com.caresync.dao.PrescriptionDAO;
import com.caresync.models.Doctor;
import com.caresync.models.MedicalHistory;
import com.caresync.models.Patient;
import com.caresync.models.Prescription;
import com.caresync.models.PrescriptionItem;
import com.caresync.models.Role;
import com.caresync.services.PdfService;
import com.caresync.services.AuditService;
import com.caresync.services.ReminderService;
import com.caresync.services.ValidationService;
import com.caresync.utils.ClinicalContext;
import com.caresync.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDateTime;

public class MedicalRecordController {
    @FXML private TextField patientSearchField;
    @FXML private ComboBox<Patient> patientCombo;
    @FXML private TableView<MedicalHistory> recordTable;
    @FXML private TableColumn<MedicalHistory, String> patientColumn;
    @FXML private TableColumn<MedicalHistory, String> doctorColumn;
    @FXML private TableColumn<MedicalHistory, String> diagnosisColumn;
    @FXML private TableColumn<MedicalHistory, Object> dateColumn;
    @FXML private TextField diagnosisField;
    @FXML private TextArea notesArea;
    @FXML private TextField medicineField;
    @FXML private TextField dosageField;
    @FXML private TextField frequencyField;
    @FXML private TextField durationField;
    @FXML private TextArea instructionsArea;
    @FXML private Label statusLabel;

    private final PatientDAO patientDAO = new PatientDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final MedicalHistoryDAO medicalHistoryDAO = new MedicalHistoryDAO();
    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();
    private final PdfService pdfService = new PdfService();
    private final ReminderService reminderService = new ReminderService();
    private final ValidationService validationService = new ValidationService();
    private final AuditService auditService = new AuditService();
    private Doctor currentDoctor;
    private Integer selectedAppointmentId;

    @FXML
    private void initialize() {
        patientColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        doctorColumn.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        diagnosisColumn.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        patientCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, patient) -> refreshRecords());
        patientSearchField.textProperty().addListener((obs, old, value) -> reloadPatientsKeepingSelection());
        try {
            if (SessionManager.getCurrentUser() != null) {
                currentDoctor = doctorDAO.findByUserId(SessionManager.getCurrentUser().getId()).orElse(null);
            }
            loadPatients();
            applyClinicalContext();
            refreshRecords();
        } catch (Exception ex) {
            setStatus("Could not initialize medical history: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void saveRecord() {
        if (currentDoctor == null) {
            setStatus("Only users linked to a doctor profile can add medical history.", false);
            return;
        }
        try {
            if (patientCombo.getValue() == null) {
                throw new IllegalArgumentException("Choose a patient.");
            }
            if (isDoctorSession() && patientCombo.getItems().stream().noneMatch(patient -> patient.getId() == patientCombo.getValue().getId())) {
                throw new IllegalArgumentException("Choose one of your assigned patients.");
            }
            if (selectedAppointmentId != null && medicalHistoryDAO.existsForAppointment(selectedAppointmentId)) {
                throw new IllegalArgumentException("A medical history record already exists for this appointment.");
            }
            validationService.requireText(diagnosisField.getText(), "Diagnosis");
            if (!medicineField.getText().isBlank()) {
                validationService.requireText(dosageField.getText(), "Dosage");
            } else if (!dosageField.getText().isBlank()) {
                validationService.requireText(medicineField.getText(), "Medicine");
            }
            Patient selectedPatient = patientCombo.getValue();
            MedicalHistory record = new MedicalHistory();
            record.setPatientId(selectedPatient.getId());
            record.setDoctorId(currentDoctor.getId());
            record.setAppointmentId(selectedAppointmentId);
            record.setDiagnosis(diagnosisField.getText().trim());
            record.setTreatmentNotes(notesArea.getText().trim());
            record.setPatientName(selectedPatient.getFullName());
            record.setDoctorName(currentDoctor.getFullName());
            record.setCreatedAt(LocalDateTime.now());
            medicalHistoryDAO.save(record);
            auditService.record("CREATE", "MEDICAL_HISTORY", record.getId(), "Medical history created for patient " + record.getPatientId() + ".");

            EmailResult prescriptionEmailResult = EmailResult.notAttempted();
            if (!medicineField.getText().isBlank()) {
                Prescription prescription = new Prescription();
                prescription.setMedicalHistoryId(record.getId());
                prescription.setInstructions(instructionsArea.getText().trim());
                PrescriptionItem item = new PrescriptionItem();
                item.setMedicineName(medicineField.getText().trim());
                item.setDosage(dosageField.getText().trim());
                item.setFrequency(frequencyField.getText().trim());
                item.setDuration(durationField.getText().trim());
                prescription.getItems().add(item);
                prescriptionDAO.saveWithItems(prescription);
                prescriptionEmailResult = sendPrescriptionPdf(selectedPatient, record, prescription);
            }

            clearForm();
            refreshRecords();
            setStatus("Medical history saved." + prescriptionEmailResult.message(), prescriptionEmailResult.success());
            ClinicalContext.clear();
            selectedAppointmentId = null;
        } catch (Exception ex) {
            setStatus("Could not save medical history: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void sendSelectedPrescriptionPdf() {
        MedicalHistory record = recordTable.getSelectionModel().getSelectedItem();
        if (record == null) {
            setStatus("Select a medical record first.", false);
            return;
        }
        try {
            Patient patient = resolvePatient(record);
            Prescription prescription = prescriptionDAO.findByMedicalHistoryId(record.getId())
                    .orElseThrow(() -> new IllegalArgumentException("This medical record has no prescription to send."));
            EmailResult result = sendPrescriptionPdf(patient, record, prescription);
            setStatus(result.message().trim(), result.success());
        } catch (Exception ex) {
            setStatus("Could not send prescription PDF: " + ex.getMessage(), false);
        }
    }

    private EmailResult sendPrescriptionPdf(Patient patient, MedicalHistory record, Prescription prescription) {
        try {
            File file = pdfService.generatePrescription(patient, record, prescription);
            reminderService.sendPrescriptionPdf(patient, record, file);
            auditService.record("PRESCRIPTION_SENT", "MEDICAL_HISTORY", record.getId(),
                    "Prescription PDF sent to " + patient.getEmail() + ".");
            return new EmailResult(true, " Prescription PDF sent to " + patient.getEmail() + ".");
        } catch (Exception ex) {
            try {
                auditService.record("PRESCRIPTION_SEND_FAILED", "MEDICAL_HISTORY", record.getId(),
                        "Prescription PDF email failed: " + ex.getMessage());
            } catch (Exception ignored) {
                // The user-facing error below is more important than audit failure here.
            }
            return new EmailResult(false, " Prescription PDF was created but email was not sent: " + ex.getMessage());
        }
    }

    @FXML
    private void generatePdf() {
        MedicalHistory record = recordTable.getSelectionModel().getSelectedItem();
        if (record == null) {
            setStatus("Select a medical record first.", false);
            return;
        }
        try {
            Patient patient = resolvePatient(record);
            Prescription prescription = prescriptionDAO.findByMedicalHistoryId(record.getId()).orElseGet(() -> {
                Prescription fallback = new Prescription();
                fallback.setMedicalHistoryId(record.getId());
                fallback.setInstructions("No prescription items were recorded.");
                return fallback;
            });
            File file = pdfService.generatePrescription(patient, record, prescription);
            auditService.record("PDF_GENERATED", "MEDICAL_HISTORY", record.getId(), "Medical report PDF generated.");
            openFile(file);
            setStatus("Full medical report PDF generated: " + file.getAbsolutePath(), true);
        } catch (Exception ex) {
            setStatus("Could not generate PDF: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void generateMedicalReportPdf() {
        MedicalHistory record = recordTable.getSelectionModel().getSelectedItem();
        if (record == null) {
            setStatus("Select a medical record first.", false);
            return;
        }
        try {
            Patient patient = resolvePatient(record);
            File file = pdfService.generateMedicalReport(patient, record);
            auditService.record("PDF_GENERATED", "MEDICAL_HISTORY", record.getId(), "Medical report-only PDF generated.");
            openFile(file);
            setStatus("Medical report PDF generated: " + file.getAbsolutePath(), true);
        } catch (Exception ex) {
            setStatus("Could not generate medical report PDF: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void generatePrescriptionOnlyPdf() {
        MedicalHistory record = recordTable.getSelectionModel().getSelectedItem();
        if (record == null) {
            setStatus("Select a medical record first.", false);
            return;
        }
        try {
            Patient patient = resolvePatient(record);
            Prescription prescription = prescriptionDAO.findByMedicalHistoryId(record.getId())
                    .orElseThrow(() -> new IllegalArgumentException("This medical record has no prescription to generate."));
            File file = pdfService.generatePrescriptionOnly(patient, record, prescription);
            auditService.record("PDF_GENERATED", "MEDICAL_HISTORY", record.getId(), "Prescription-only PDF generated.");
            openFile(file);
            setStatus("Prescription PDF generated: " + file.getAbsolutePath(), true);
        } catch (Exception ex) {
            setStatus("Could not generate prescription PDF: " + ex.getMessage(), false);
        }
    }

    private void openFile(File file) throws java.io.IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file);
        }
    }

    private Patient resolvePatient(MedicalHistory record) throws java.sql.SQLException {
        Patient selected = patientCombo.getValue();
        if (selected != null && selected.getId() == record.getPatientId()) {
            return selected;
        }
        return patientDAO.findById(record.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Could not find the patient for this medical record."));
    }

    @FXML
    private void clearForm() {
        diagnosisField.clear();
        notesArea.clear();
        medicineField.clear();
        dosageField.clear();
        frequencyField.clear();
        durationField.clear();
        instructionsArea.clear();
    }

    private void refreshRecords() {
        try {
            if (patientCombo.getValue() != null) {
                if (isDoctorSession()) {
                    recordTable.setItems(FXCollections.observableArrayList(
                            medicalHistoryDAO.findByPatientIdAndDoctorUserId(
                                    patientCombo.getValue().getId(),
                                    SessionManager.getCurrentUser().getId()
                            )
                    ));
                } else {
                    recordTable.setItems(FXCollections.observableArrayList(
                            medicalHistoryDAO.findByPatientId(patientCombo.getValue().getId())
                    ));
                }
            } else if (SessionManager.getCurrentUser() != null) {
                if (isDoctorSession()) {
                    recordTable.setItems(FXCollections.observableArrayList(
                            medicalHistoryDAO.findByDoctorUserId(SessionManager.getCurrentUser().getId())
                    ));
                } else {
                    recordTable.setItems(FXCollections.observableArrayList());
                }
            }
        } catch (Exception ex) {
            setStatus("Could not load records: " + ex.getMessage(), false);
        }
    }

    private void loadPatients() throws java.sql.SQLException {
        loadPatients(patientSearchField == null ? "" : patientSearchField.getText());
    }

    private void loadPatients(String search) throws java.sql.SQLException {
        if (isDoctorSession()) {
            patientCombo.setItems(FXCollections.observableArrayList(
                    patientDAO.findByDoctorUserId(SessionManager.getCurrentUser().getId(), search)
            ));
            return;
        }
        patientCombo.setItems(FXCollections.observableArrayList(patientDAO.findAll(search)));
    }

    private void reloadPatientsKeepingSelection() {
        Patient selected = patientCombo.getValue();
        try {
            loadPatients(patientSearchField.getText());
            if (selected != null) {
                patientCombo.getItems().stream()
                        .filter(patient -> patient.getId() == selected.getId())
                        .findFirst()
                        .ifPresentOrElse(patientCombo::setValue, () -> patientCombo.setValue(null));
            }
            if (patientCombo.getItems().size() == 1 && patientCombo.getValue() == null) {
                patientCombo.setValue(patientCombo.getItems().get(0));
            }
        } catch (Exception ex) {
            setStatus("Could not search patients: " + ex.getMessage(), false);
        }
    }

    private boolean isDoctorSession() {
        return SessionManager.getCurrentUser() != null
                && SessionManager.getCurrentUser().getRole() == Role.DOCTOR;
    }

    private void applyClinicalContext() {
        Integer patientId = ClinicalContext.getSelectedPatientId();
        selectedAppointmentId = ClinicalContext.getSelectedAppointmentId();
        if (patientId == null) {
            return;
        }
        patientCombo.getItems().stream()
                .filter(patient -> patient.getId() == patientId)
                .findFirst()
                .ifPresent(patientCombo::setValue);
        if (selectedAppointmentId != null) {
            setStatus("Clinical record opened for selected appointment #" + selectedAppointmentId + ".", true);
        }
    }

    private void setStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-text", "error-text");
        statusLabel.getStyleClass().add(success ? "success-text" : "error-text");
    }

    private record EmailResult(boolean success, String message) {
        private static EmailResult notAttempted() {
            return new EmailResult(true, "");
        }
    }
}
