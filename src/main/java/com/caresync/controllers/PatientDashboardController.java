package com.caresync.controllers;

import com.caresync.dao.AppointmentDAO;
import com.caresync.dao.MedicalHistoryDAO;
import com.caresync.dao.PatientDAO;
import com.caresync.dao.PrescriptionDAO;
import com.caresync.models.Appointment;
import com.caresync.models.AppointmentStatus;
import com.caresync.models.MedicalHistory;
import com.caresync.models.Patient;
import com.caresync.models.Prescription;
import com.caresync.services.PdfService;
import com.caresync.services.AuditService;
import com.caresync.utils.AnimationUtil;
import com.caresync.utils.SessionManager;
import com.caresync.utils.UiStyleUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.util.Comparator;

public class PatientDashboardController {
    @FXML private Label profileLabel;
    @FXML private Label nextReminderLabel;
    @FXML private Label appointmentCountLabel;
    @FXML private Label historyCountLabel;
    @FXML private ProgressBar reminderProgress;
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, String> doctorColumn;
    @FXML private TableColumn<Appointment, Object> dateColumn;
    @FXML private TableColumn<Appointment, Object> timeColumn;
    @FXML private TableColumn<Appointment, AppointmentStatus> statusColumn;
    @FXML private TableView<MedicalHistory> historyTable;
    @FXML private TableColumn<MedicalHistory, String> diagnosisColumn;
    @FXML private TableColumn<MedicalHistory, String> historyDoctorColumn;
    @FXML private TableColumn<MedicalHistory, Object> historyDateColumn;
    @FXML private Label statusLabel;

    private final PatientDAO patientDAO = new PatientDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final MedicalHistoryDAO medicalHistoryDAO = new MedicalHistoryDAO();
    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();
    private final PdfService pdfService = new PdfService();
    private final AuditService auditService = new AuditService();
    private Patient currentPatient;

    @FXML
    private void initialize() {
        doctorColumn.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentDate"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiStyleUtil.applyAppointmentStatusCell(statusColumn);
        diagnosisColumn.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));
        historyDoctorColumn.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        historyDateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        UiStyleUtil.applyEmptyState(appointmentTable, "No appointments on your profile yet.");
        UiStyleUtil.applyEmptyState(historyTable, "No medical history has been recorded yet.");
        refresh();
    }

    @FXML
    private void generatePrescriptionPdf() {
        MedicalHistory history = historyTable.getSelectionModel().getSelectedItem();
        if (currentPatient == null || history == null) {
            setStatus("Select a medical history entry first.", false);
            return;
        }
        try {
            Prescription prescription = prescriptionDAO.findByMedicalHistoryId(history.getId()).orElseGet(() -> {
                Prescription fallback = new Prescription();
                fallback.setMedicalHistoryId(history.getId());
                fallback.setInstructions("No prescription items were recorded.");
                return fallback;
            });
            File file = pdfService.generatePrescription(currentPatient, history, prescription);
            auditService.record("PDF_GENERATED", "MEDICAL_HISTORY", history.getId(), "Patient generated medical report PDF.");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
            setStatus("Medical report PDF generated: " + file.getAbsolutePath(), true);
        } catch (Exception ex) {
            setStatus("Could not generate PDF: " + ex.getMessage(), false);
        }
    }

    private void refresh() {
        try {
            int userId = SessionManager.getCurrentUser().getId();
            currentPatient = patientDAO.findByUserId(userId).orElse(null);
            if (currentPatient == null) {
                profileLabel.setText("No patient profile is linked to this account yet.");
                return;
            }
            profileLabel.setText(currentPatient.getFullName() + " | " + currentPatient.getGenderLabel()
                    + " | " + nullToEmpty(currentPatient.getPhone()));
            var appointments = appointmentDAO.findByPatientUserId(userId);
            appointmentTable.setItems(FXCollections.observableArrayList(appointments));
            AnimationUtil.countTo(appointmentCountLabel, appointments.size());
            var nextAppointment = appointments.stream()
                    .filter(appointment -> !appointment.getAppointmentDate().isBefore(LocalDate.now()))
                    .filter(appointment -> appointment.getStatus() == AppointmentStatus.SCHEDULED
                            || appointment.getStatus() == AppointmentStatus.PENDING)
                    .min(Comparator.comparing(Appointment::getAppointmentDate)
                            .thenComparing(Appointment::getAppointmentTime));
            nextAppointment.ifPresentOrElse(
                    appointment -> nextReminderLabel.setText("Reminder: " + appointment.getAppointmentDate()
                            + " at " + appointment.getAppointmentTime() + " with Dr. " + appointment.getDoctorName()),
                    () -> nextReminderLabel.setText("No appointment reminders yet.")
            );
            reminderProgress.setProgress(nextAppointment.isPresent() ? 1 : 0);
            var history = medicalHistoryDAO.findByPatientUserId(userId);
            historyTable.setItems(FXCollections.observableArrayList(history));
            AnimationUtil.countTo(historyCountLabel, history.size());
        } catch (Exception ex) {
            setStatus("Could not load patient dashboard: " + ex.getMessage(), false);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void setStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-text", "error-text");
        statusLabel.getStyleClass().add(success ? "success-text" : "error-text");
    }
}
