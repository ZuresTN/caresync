package com.caresync.controllers;

import com.caresync.Main;
import com.caresync.dao.AppointmentDAO;
import com.caresync.dao.DoctorDAO;
import com.caresync.models.Appointment;
import com.caresync.models.AppointmentStatus;
import com.caresync.utils.AnimationUtil;
import com.caresync.utils.ClinicalContext;
import com.caresync.utils.SessionManager;
import com.caresync.utils.UiStyleUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

public class DoctorDashboardController {
    @FXML private Label appointmentCountLabel;
    @FXML private Label pendingAppointmentsLabel;
    @FXML private Label completedAppointmentsLabel;
    @FXML private Label doctorInfoLabel;
    @FXML private Label focusStatusLabel;
    @FXML private ProgressBar focusProgress;
    @FXML private ProgressBar clinicalProgress;
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, String> patientColumn;
    @FXML private TableColumn<Appointment, Object> dateColumn;
    @FXML private TableColumn<Appointment, Object> timeColumn;
    @FXML private TableColumn<Appointment, AppointmentStatus> statusColumn;
    @FXML private Label actionStatusLabel;

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();

    @FXML
    private void initialize() {
        patientColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentDate"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiStyleUtil.applyAppointmentStatusCell(statusColumn);
        UiStyleUtil.applyEmptyState(appointmentTable, "No appointments assigned yet.");
        refresh();
    }

    private void refresh() {
        try {
            int userId = SessionManager.getCurrentUser().getId();
            var appointments = appointmentDAO.findByDoctorUserId(userId);
            int open = (int) appointments.stream()
                    .filter(appointment -> appointment.getStatus() == AppointmentStatus.PENDING
                            || appointment.getStatus() == AppointmentStatus.SCHEDULED)
                    .count();
            int completed = (int) appointments.stream()
                    .filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                    .count();
            appointmentTable.setItems(FXCollections.observableArrayList(appointments));
            AnimationUtil.countTo(appointmentCountLabel, appointments.size());
            AnimationUtil.countTo(pendingAppointmentsLabel, open);
            AnimationUtil.countTo(completedAppointmentsLabel, completed);
            double progress = appointments.isEmpty() ? 0 : (double) completed / appointments.size();
            focusProgress.setProgress(progress);
            clinicalProgress.setProgress(progress);
            focusStatusLabel.setText(open == 0
                    ? "No open visits waiting in your queue."
                    : open + " open visit" + (open == 1 ? "" : "s") + " need clinical attention.");
            doctorInfoLabel.setText(doctorDAO.findByUserId(userId)
                    .map(doctor -> doctor.getSpecialization() + " | " + doctor.getAvailability())
                    .orElse("Doctor account needs a linked clinical profile."));
        } catch (Exception ex) {
            appointmentCountLabel.setText("!");
        }
    }

    @FXML
    private void openSelectedMedicalRecord() {
        Appointment appointment = appointmentTable.getSelectionModel().getSelectedItem();
        if (appointment == null) {
            setActionStatus("Select an appointment first.");
            return;
        }
        try {
            ClinicalContext.selectAppointment(appointment.getPatientId(), appointment.getId());
            Node page = Main.loadFXML("medical_record");
            StackPane contentPane = (StackPane) appointmentTable.getScene().getRoot().lookup(".content-pane");
            if (contentPane == null) {
                setActionStatus("Could not open medical record from this screen.");
                return;
            }
            contentPane.getChildren().setAll(page);
            AnimationUtil.pageEnter(page).play();
        } catch (Exception ex) {
            setActionStatus("Could not open medical record: " + ex.getMessage());
        }
    }

    private void setActionStatus(String message) {
        if (actionStatusLabel != null) {
            actionStatusLabel.setText(message);
            actionStatusLabel.getStyleClass().removeAll("success-text", "error-text");
            actionStatusLabel.getStyleClass().add("error-text");
        }
    }
}
