package com.caresync.controllers;

import com.caresync.dao.AppointmentDAO;
import com.caresync.dao.DoctorDAO;
import com.caresync.dao.PatientDAO;
import com.caresync.dao.ReceptionistDAO;
import com.caresync.dao.UserDAO;
import com.caresync.models.AppointmentStatus;
import com.caresync.utils.AnimationUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class AdminDashboardController {
    @FXML private Label totalUsersLabel;
    @FXML private Label totalDoctorsLabel;
    @FXML private Label totalReceptionistsLabel;
    @FXML private Label totalPatientsLabel;
    @FXML private Label todayAppointmentsLabel;
    @FXML private Label pendingAppointmentsLabel;
    @FXML private Label scheduledAppointmentsLabel;
    @FXML private Label completedAppointmentsLabel;
    @FXML private Label openPipelineLabel;
    @FXML private Label heroOpenPipelineLabel;
    @FXML private Label operationStatusLabel;
    @FXML private ProgressBar operationProgress;
    @FXML private ProgressBar workforceProgress;
    @FXML private ProgressBar pipelineProgress;
    private final UserDAO userDAO = new UserDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final ReceptionistDAO receptionistDAO = new ReceptionistDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    @FXML
    private void initialize() {
        operationProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        workforceProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        pipelineProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        loadMetricsAsync();
    }

    private void loadMetricsAsync() {
        Task<DashboardMetrics> task = new Task<>() {
            @Override
            protected DashboardMetrics call() throws Exception {
                int users = userDAO.countAll();
                int doctors = doctorDAO.countAll();
                int receptionists = receptionistDAO.countAll();
                int patients = patientDAO.countAll();
                int today = appointmentDAO.countToday();
                int pending = appointmentDAO.countByStatus(AppointmentStatus.PENDING);
                int scheduled = appointmentDAO.countByStatus(AppointmentStatus.SCHEDULED);
                int completed = appointmentDAO.countByStatus(AppointmentStatus.COMPLETED);
                return new DashboardMetrics(users, doctors, receptionists, patients, today, pending, scheduled, completed);
            }
        };

        task.setOnSucceeded(event -> applyMetrics(task.getValue()));
        task.setOnFailed(event -> {
            totalUsersLabel.setText("!");
            openPipelineLabel.setText("!");
            operationStatusLabel.setText("Dashboard data could not be loaded.");
            operationProgress.setProgress(0);
            workforceProgress.setProgress(0);
            pipelineProgress.setProgress(0);
        });

        Thread worker = new Thread(task, "admin-dashboard-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyMetrics(DashboardMetrics metrics) {
        AnimationUtil.countTo(totalUsersLabel, metrics.users());
        AnimationUtil.countTo(totalDoctorsLabel, metrics.doctors());
        AnimationUtil.countTo(totalReceptionistsLabel, metrics.receptionists());
        AnimationUtil.countTo(totalPatientsLabel, metrics.patients());
        AnimationUtil.countTo(todayAppointmentsLabel, metrics.today());
        AnimationUtil.countTo(pendingAppointmentsLabel, metrics.pending());
        AnimationUtil.countTo(scheduledAppointmentsLabel, metrics.scheduled());
        AnimationUtil.countTo(completedAppointmentsLabel, metrics.completed());
        AnimationUtil.countTo(openPipelineLabel, metrics.pending() + metrics.scheduled());
        AnimationUtil.countTo(heroOpenPipelineLabel, metrics.pending() + metrics.scheduled());

        int activePipeline = metrics.pending() + metrics.scheduled() + metrics.completed();
        double pipelineRatio = ratio(metrics.completed(), activePipeline);
        double staffingRatio = ratio(metrics.doctors() + metrics.receptionists(), Math.max(1, metrics.today()));
        AnimationUtil.animateProgress(pipelineProgress, pipelineRatio);
        AnimationUtil.animateProgress(workforceProgress, staffingRatio);
        AnimationUtil.animateProgress(operationProgress, activePipeline == 0 ? 1 : Math.max(0.18, pipelineRatio));
        operationStatusLabel.setText(metrics.today() == 0
                ? "No appointments scheduled for today."
                : metrics.today() + " appointment" + (metrics.today() == 1 ? "" : "s") + " on today's board.");
    }

    private double ratio(int value, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min(1, Math.max(0, (double) value / total));
    }

    private record DashboardMetrics(
            int users,
            int doctors,
            int receptionists,
            int patients,
            int today,
            int pending,
            int scheduled,
            int completed
    ) {
    }
}
