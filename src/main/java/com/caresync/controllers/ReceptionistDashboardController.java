package com.caresync.controllers;

import com.caresync.dao.AppointmentDAO;
import com.caresync.dao.PatientDAO;
import com.caresync.models.Appointment;
import com.caresync.models.AppointmentStatus;
import com.caresync.utils.AnimationUtil;
import com.caresync.utils.UiStyleUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;

public class ReceptionistDashboardController {
    @FXML private Label totalPatientsLabel;
    @FXML private Label todayAppointmentsLabel;
    @FXML private Label scheduledLabel;
    @FXML private Label pendingAppointmentsLabel;
    @FXML private Label reminderQueueLabel;
    @FXML private Label flowStatusLabel;
    @FXML private ProgressBar flowProgress;
    @FXML private ProgressBar queueProgress;
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, String> patientColumn;
    @FXML private TableColumn<Appointment, String> doctorColumn;
    @FXML private TableColumn<Appointment, Object> timeColumn;
    @FXML private TableColumn<Appointment, AppointmentStatus> statusColumn;

    private final PatientDAO patientDAO = new PatientDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    @FXML
    private void initialize() {
        patientColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        doctorColumn.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiStyleUtil.applyAppointmentStatusCell(statusColumn);
        UiStyleUtil.applyEmptyState(appointmentTable, "No appointments scheduled for today.");
        refresh();
    }

    private void refresh() {
        try {
            int patients = patientDAO.countAll();
            int today = appointmentDAO.countToday();
            int scheduled = appointmentDAO.countByStatus(AppointmentStatus.SCHEDULED);
            int pending = appointmentDAO.countByStatus(AppointmentStatus.PENDING);
            var todayAppointments = appointmentDAO.findAll("", null, LocalDate.now());
            int todayScheduled = appointmentDAO.findAll("", AppointmentStatus.SCHEDULED, LocalDate.now()).size();

            AnimationUtil.countTo(totalPatientsLabel, patients);
            AnimationUtil.countTo(todayAppointmentsLabel, today);
            AnimationUtil.countTo(scheduledLabel, scheduled);
            AnimationUtil.countTo(pendingAppointmentsLabel, pending);
            AnimationUtil.countTo(reminderQueueLabel, todayScheduled);
            appointmentTable.setItems(FXCollections.observableArrayList(todayAppointments));
            double scheduledRatio = todayAppointments.isEmpty() ? 0 : (double) todayScheduled / todayAppointments.size();
            flowProgress.setProgress(todayAppointments.isEmpty() ? 1 : Math.max(0.15, scheduledRatio));
            queueProgress.setProgress(todayAppointments.isEmpty() ? 0 : scheduledRatio);
            flowStatusLabel.setText(todayAppointments.isEmpty()
                    ? "No appointments on today's board."
                    : todayAppointments.size() + " appointment" + (todayAppointments.size() == 1 ? "" : "s") + " to coordinate today.");
        } catch (Exception ex) {
            totalPatientsLabel.setText("!");
            flowStatusLabel.setText("Dashboard data could not be loaded.");
        }
    }
}
