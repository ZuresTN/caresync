package com.caresync.controllers;

import com.caresync.dao.AppointmentDAO;
import com.caresync.dao.DoctorDAO;
import com.caresync.dao.PatientDAO;
import com.caresync.models.Appointment;
import com.caresync.models.AppointmentStatus;
import com.caresync.models.Doctor;
import com.caresync.models.Patient;
import com.caresync.models.Role;
import com.caresync.services.ReminderService;
import com.caresync.services.AppointmentValidationService;
import com.caresync.services.AuditService;
import com.caresync.services.GoogleCalendarService;
import com.caresync.utils.AnimationUtil;
import com.caresync.utils.SessionManager;
import com.caresync.utils.UiStyleUtil;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppointmentManagementController {
    private static final Logger LOGGER = Logger.getLogger(AppointmentManagementController.class.getName());
    @FXML private TextField searchField;
    @FXML private ChoiceBox<AppointmentStatus> filterStatusChoice;
    @FXML private DatePicker filterDatePicker;
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, String> patientColumn;
    @FXML private TableColumn<Appointment, String> doctorColumn;
    @FXML private TableColumn<Appointment, Object> dateColumn;
    @FXML private TableColumn<Appointment, Object> timeColumn;
    @FXML private TableColumn<Appointment, AppointmentStatus> statusColumn;
    @FXML private ComboBox<Patient> patientCombo;
    @FXML private ComboBox<Doctor> doctorCombo;
    @FXML private DatePicker appointmentDatePicker;
    @FXML private ChoiceBox<String> timeChoice;
    @FXML private ChoiceBox<AppointmentStatus> statusChoice;
    @FXML private TextField reasonField;
    @FXML private TextArea notesArea;
    @FXML private Label availabilityLabel;
    @FXML private Label statusLabel;
    @FXML private Label calendarTitleLabel;
    @FXML private GridPane calendarGrid;
    @FXML private Button saveAppointmentButton;
    @FXML private Button clearFormButton;
    @FXML private Button checkAvailabilityButton;
    @FXML private Button openFormGoogleButton;
    @FXML private Button queueReminderButton;
    @FXML private Button sendDueRemindersButton;
    @FXML private Button completeAppointmentButton;
    @FXML private Button cancelAppointmentButton;

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final ReminderService reminderService = new ReminderService();
    private final AppointmentValidationService appointmentValidationService = new AppointmentValidationService();
    private final AuditService auditService = new AuditService();
    private final GoogleCalendarService googleCalendarService = new GoogleCalendarService();
    private Appointment selectedAppointment;
    private LocalDate calendarFocusDate = LocalDate.now();

    @FXML
    private void initialize() {
        patientColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        doctorColumn.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentDate"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        UiStyleUtil.applyAppointmentStatusCell(statusColumn);
        filterStatusChoice.setItems(FXCollections.observableArrayList(AppointmentStatus.values()));
        statusChoice.setItems(FXCollections.observableArrayList(AppointmentStatus.values()));
        statusChoice.setValue(AppointmentStatus.SCHEDULED);
        for (int hour = 8; hour <= 17; hour++) {
            timeChoice.getItems().add(String.format("%02d:00", hour));
            timeChoice.getItems().add(String.format("%02d:30", hour));
        }
        setDefaultAppointmentDateTime();
        appointmentTable.getSelectionModel().selectedItemProperty().addListener((obs, old, appointment) -> selectAppointment(appointment));
        doctorCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, doctor) ->
                availabilityLabel.setText(doctor == null ? "" : doctor.getAvailability()));
        searchField.textProperty().addListener((obs, old, value) -> refresh());
        filterStatusChoice.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> refresh());
        filterDatePicker.valueProperty().addListener((obs, old, value) -> {
            if (value != null) {
                calendarFocusDate = value;
            }
            refresh();
        });
        loadLookups();
        configureRoleAccess();
        refresh();
    }

    @FXML
    private void saveAppointment() {
        if (isDoctorSession()) {
            setStatus("Doctor accounts can view assigned appointments only.", false);
            return;
        }
        if (patientCombo.getValue() == null || doctorCombo.getValue() == null
                || appointmentDatePicker.getValue() == null || timeChoice.getValue() == null) {
            setStatus("Patient, doctor, date, and time are required.", false);
            return;
        }
        try {
            Appointment appointment = selectedAppointment == null ? new Appointment() : selectedAppointment;
            boolean creating = selectedAppointment == null;
            appointment.setPatientId(patientCombo.getValue().getId());
            appointment.setDoctorId(doctorCombo.getValue().getId());
            appointment.setAppointmentDate(appointmentDatePicker.getValue());
            appointment.setAppointmentTime(LocalTime.parse(timeChoice.getValue()));
            appointment.setStatus(statusChoice.getValue());
            appointment.setReason(reasonField.getText().trim());
            appointment.setNotes(notesArea.getText().trim());
            appointmentValidationService.validateForSave(appointment, SessionManager.getCurrentUser().getRole());
            appointmentDAO.save(appointment);
            filterDatePicker.setValue(appointment.getAppointmentDate());
            filterStatusChoice.setValue(null);
            clearForm();
            refresh();
            auditService.record(creating ? "CREATE" : "UPDATE", "APPOINTMENT", appointment.getId(),
                    "Appointment saved for " + appointment.getAppointmentDate() + " " + appointment.getAppointmentTime() + ".");
            setStatus("Appointment saved for " + appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() + ".", true);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Could not save appointment", ex);
            setStatus("Could not save appointment: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void cancelAppointment() {
        updateSelectedStatus(AppointmentStatus.CANCELLED);
    }

    @FXML
    private void completeAppointment() {
        updateSelectedStatus(AppointmentStatus.COMPLETED);
    }

    @FXML
    private void sendReminder() {
        if (selectedAppointment == null) {
            setStatus("Select an appointment first.", false);
            return;
        }
        setStatus(reminderService.buildReminder(selectedAppointment), true);
    }

    @FXML
    private void checkDoctorAvailability() {
        Doctor doctor = doctorCombo.getValue();
        if (doctor == null) {
            setStatus("Select a doctor first.", false);
            return;
        }
        setStatus("Availability for " + doctor.getFullName() + ": " + doctor.getAvailability(), true);
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        filterStatusChoice.setValue(null);
        filterDatePicker.setValue(null);
        refresh();
    }

    @FXML
    private void clearForm() {
        selectedAppointment = null;
        patientCombo.setValue(null);
        doctorCombo.setValue(null);
        setDefaultAppointmentDateTime();
        statusChoice.setValue(AppointmentStatus.SCHEDULED);
        reasonField.clear();
        notesArea.clear();
        availabilityLabel.setText("");
    }

    private void updateSelectedStatus(AppointmentStatus status) {
        if (isDoctorSession()) {
            setStatus("Doctor accounts can view assigned appointments only.", false);
            return;
        }
        if (selectedAppointment == null) {
            return;
        }
        try {
            appointmentDAO.updateStatus(selectedAppointment.getId(), status);
            auditService.record("STATUS_CHANGE", "APPOINTMENT", selectedAppointment.getId(),
                    "Appointment marked " + status.name().toLowerCase() + ".");
            refresh();
            setStatus("Appointment marked " + status.name().toLowerCase() + ".", true);
        } catch (Exception ex) {
            setStatus("Could not update status: " + ex.getMessage(), false);
        }
    }

    private void selectAppointment(Appointment appointment) {
        selectedAppointment = appointment;
        if (appointment == null) {
            return;
        }
        patientCombo.getItems().stream()
                .filter(patient -> patient.getId() == appointment.getPatientId())
                .findFirst()
                .ifPresent(patientCombo::setValue);
        doctorCombo.getItems().stream()
                .filter(doctor -> doctor.getId() == appointment.getDoctorId())
                .findFirst()
                .ifPresent(doctorCombo::setValue);
        appointmentDatePicker.setValue(appointment.getAppointmentDate());
        timeChoice.setValue(appointment.getAppointmentTime().toString().substring(0, 5));
        statusChoice.setValue(appointment.getStatus());
        reasonField.setText(appointment.getReason());
        notesArea.setText(appointment.getNotes());
    }

    @FXML
    private void openSelectedInGoogleCalendar() {
        if (selectedAppointment == null) {
            setStatus("Select an appointment first.", false);
            return;
        }
        openGoogleCalendar(selectedAppointment);
    }

    @FXML
    private void openFormInGoogleCalendar() {
        if (isDoctorSession()) {
            openSelectedInGoogleCalendar();
            return;
        }
        if (patientCombo.getValue() == null || doctorCombo.getValue() == null
                || appointmentDatePicker.getValue() == null || timeChoice.getValue() == null) {
            setStatus("Patient, doctor, date, and time are required for Google Calendar.", false);
            return;
        }
        Appointment appointment = selectedAppointment == null ? new Appointment() : selectedAppointment;
        appointment.setPatientId(patientCombo.getValue().getId());
        appointment.setDoctorId(doctorCombo.getValue().getId());
        appointment.setPatientName(patientCombo.getValue().getFullName());
        appointment.setPatientEmail(patientCombo.getValue().getEmail());
        appointment.setPatientPhone(patientCombo.getValue().getPhone());
        appointment.setDoctorName(doctorCombo.getValue().getFullName());
        appointment.setAppointmentDate(appointmentDatePicker.getValue());
        appointment.setAppointmentTime(LocalTime.parse(timeChoice.getValue()));
        appointment.setReason(reasonField.getText().trim());
        appointment.setNotes(notesArea.getText().trim());
        openGoogleCalendar(appointment);
    }

    private void openGoogleCalendar(Appointment appointment) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IllegalStateException("Desktop browser integration is not available.");
            }
            Desktop.getDesktop().browse(googleCalendarService.buildAppointmentUri(appointment));
            setStatus("Google Calendar opened with this appointment prefilled.", true);
        } catch (Exception ex) {
            setStatus("Could not open Google Calendar: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void queueReminder() {
        if (isDoctorSession()) {
            setStatus("Doctor accounts can view assigned appointments only.", false);
            return;
        }
        if (selectedAppointment == null) {
            setStatus("Select an appointment first.", false);
            return;
        }
        try {
            reminderService.queueReminder(selectedAppointment);
            auditService.record("REMINDER_QUEUED", "APPOINTMENT", selectedAppointment.getId(),
                    "Reminder queued for " + selectedAppointment.getPatientName() + ".");
            setStatus("Reminder queued to " + selectedAppointment.getPatientEmail() + ".", true);
        } catch (Exception ex) {
            setStatus("Could not queue reminder: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void sendReminderNow() {
        if (isDoctorSession()) {
            setStatus("Doctor accounts can view assigned appointments only.", false);
            return;
        }
        if (selectedAppointment == null) {
            setStatus("Select an appointment first.", false);
            return;
        }
        try {
            reminderService.sendAppointmentReminderNow(selectedAppointment);
            auditService.record("REMINDER_SENT_NOW", "APPOINTMENT", selectedAppointment.getId(),
                    "Reminder sent immediately to " + selectedAppointment.getPatientEmail() + ".");
            setStatus("Reminder sent now to " + selectedAppointment.getPatientEmail() + ".", true);
        } catch (Exception ex) {
            setStatus("Could not send reminder now: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void sendDueReminders() {
        if (isDoctorSession()) {
            setStatus("Doctor accounts can view assigned appointments only.", false);
            return;
        }
        try {
            if (!reminderService.isSmtpEnabled()) {
                setStatus("SMTP is disabled. Pending reminders were left in the queue.", true);
                return;
            }
            int sent = reminderService.sendDueReminders();
            auditService.record("REMINDERS_SENT", "REMINDER", null, sent + " due reminder(s) sent.");
            setStatus(sent + " due reminder(s) sent. Failed reminders stay in the queue with an error.", true);
        } catch (Exception ex) {
            setStatus("Could not send reminders: " + ex.getMessage(), false);
        }
    }

    private void loadLookups() {
        try {
            if (isDoctorSession()) {
                int userId = SessionManager.getCurrentUser().getId();
                patientCombo.setItems(FXCollections.observableArrayList(patientDAO.findByDoctorUserId(userId, "")));
                doctorDAO.findByUserId(userId).ifPresent(doctor -> {
                    doctorCombo.setItems(FXCollections.observableArrayList(doctor));
                    doctorCombo.setValue(doctor);
                    availabilityLabel.setText(doctor.getAvailability());
                });
            } else {
                patientCombo.setItems(FXCollections.observableArrayList(patientDAO.findAll("")));
                doctorCombo.setItems(FXCollections.observableArrayList(doctorDAO.findAll()));
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Could not load lookup data", ex);
            setStatus("Could not load lookup data: " + ex.getMessage(), false);
        }
    }

    private void refresh() {
        try {
            LocalDate focusDate = calendarFocusDate == null ? LocalDate.now() : calendarFocusDate;
            YearMonth month = YearMonth.from(focusDate);
            List<Appointment> appointments;
            List<Appointment> calendarAppointments;
            if (isDoctorSession()) {
                int doctorUserId = SessionManager.getCurrentUser().getId();
                appointments = appointmentDAO.findByDoctorUserId(
                        doctorUserId,
                        searchField.getText(),
                        filterStatusChoice.getValue(),
                        filterDatePicker.getValue()
                );
                calendarAppointments = appointmentDAO.findByDoctorUserIdBetween(
                        doctorUserId,
                        searchField.getText(),
                        filterStatusChoice.getValue(),
                        month.atDay(1),
                        month.atEndOfMonth()
                );
            } else {
                appointments = appointmentDAO.findAll(
                        searchField.getText(),
                        filterStatusChoice.getValue(),
                        filterDatePicker.getValue()
                );
                calendarAppointments = appointmentDAO.findAllBetween(
                        searchField.getText(),
                        filterStatusChoice.getValue(),
                        month.atDay(1),
                        month.atEndOfMonth()
                );
            }
            appointmentTable.setItems(FXCollections.observableArrayList(appointments));
            renderCalendar(calendarAppointments, focusDate);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Could not load appointments", ex);
            setStatus("Could not load appointments: " + ex.getMessage(), false);
        }
    }

    private void setDefaultAppointmentDateTime() {
        LocalDate date = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime nextSlot = now.plusMinutes(now.getMinute() < 30 ? 30 - now.getMinute() : 60 - now.getMinute())
                .withSecond(0)
                .withNano(0);
        if (nextSlot.isBefore(LocalTime.of(8, 0))) {
            nextSlot = LocalTime.of(8, 0);
        }
        if (nextSlot.isAfter(LocalTime.of(17, 30))) {
            date = date.plusDays(1);
            nextSlot = LocalTime.of(8, 0);
        }
        appointmentDatePicker.setValue(date);
        timeChoice.setValue(nextSlot.toString().substring(0, 5));
    }

    @FXML
    private void previousMonth() {
        LocalDate focusDate = calendarFocusDate == null ? LocalDate.now() : calendarFocusDate;
        calendarFocusDate = focusDate.minusMonths(1).withDayOfMonth(1);
        refresh();
    }

    @FXML
    private void nextMonth() {
        LocalDate focusDate = calendarFocusDate == null ? LocalDate.now() : calendarFocusDate;
        calendarFocusDate = focusDate.plusMonths(1).withDayOfMonth(1);
        refresh();
    }

    @FXML
    private void showToday() {
        LocalDate today = LocalDate.now();
        calendarFocusDate = today;
        filterDatePicker.setValue(today);
        refresh();
    }

    private void renderCalendar(List<Appointment> appointments, LocalDate focusDate) {
        calendarGrid.getChildren().clear();
        List<VBox> calendarCells = new ArrayList<>();
        YearMonth month = YearMonth.from(focusDate);
        if (calendarTitleLabel != null) {
            calendarTitleLabel.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.getYear());
        }

        for (int i = 0; i < 7; i++) {
            Label header = new Label(java.time.DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT, Locale.getDefault()).toUpperCase());
            header.getStyleClass().add("calendar-column-header");
            calendarGrid.add(header, i, 0);
        }

        LocalDate firstOfMonth = month.atDay(1);
        LocalDate gridStart = firstOfMonth.minusDays(firstOfMonth.getDayOfWeek().getValue() - 1L);
        for (int i = 0; i < 42; i++) {
            LocalDate day = gridStart.plusDays(i);
            VBox cell = new VBox(6);
            cell.getStyleClass().add("calendar-cell");
            if (!YearMonth.from(day).equals(month)) {
                cell.getStyleClass().add("calendar-cell-muted");
            }
            if (day.equals(LocalDate.now())) {
                cell.getStyleClass().add("calendar-cell-today");
            }
            cell.setPadding(new Insets(10));
            cell.setOnMouseClicked(event -> {
                calendarFocusDate = day;
                filterDatePicker.setValue(day);
            });
            Label title = new Label(String.valueOf(day.getDayOfMonth()));
            title.getStyleClass().add("calendar-day");
            cell.getChildren().add(title);
            List<Appointment> dayAppointments = appointments.stream()
                    .filter(appointment -> day.equals(appointment.getAppointmentDate()))
                    .sorted((first, second) -> first.getAppointmentTime().compareTo(second.getAppointmentTime()))
                    .toList();
            dayAppointments.stream()
                    .limit(3)
                    .forEach(appointment -> {
                        Label badge = new Label(formatCalendarBadge(appointment));
                        badge.getStyleClass().addAll("calendar-badge", "calendar-badge-" + appointment.getStatus().name().toLowerCase());
                        badge.setMaxWidth(Double.MAX_VALUE);
                        badge.setTooltip(new Tooltip(appointment.getAppointmentTime() + " | "
                                + appointment.getPatientName() + " with " + appointment.getDoctorName()
                                + " | " + appointment.getStatus().name()));
                        badge.setOnMouseClicked(event -> {
                            event.consume();
                            selectAppointment(appointment);
                            selectAppointmentInTable(appointment);
                        });
                        cell.getChildren().add(badge);
                    });
            if (dayAppointments.size() > 3) {
                Label more = new Label("+" + (dayAppointments.size() - 3) + " more");
                more.getStyleClass().add("calendar-more");
                cell.getChildren().add(more);
            }
            if (!dayAppointments.isEmpty()) {
                cell.setOnMouseClicked(event -> {
                    calendarFocusDate = day;
                    showDayAppointmentsPopup(cell, day, dayAppointments);
                });
            }
            calendarGrid.add(cell, i % 7, (i / 7) + 1);
            calendarCells.add(cell);
        }
        Platform.runLater(() -> AnimationUtil.revealFastGrid(calendarCells));
    }

    private String formatCalendarBadge(Appointment appointment) {
        String patientName = appointment.getPatientName() == null ? "Patient" : appointment.getPatientName();
        return appointment.getAppointmentTime().toString().substring(0, 5) + " " + patientName;
    }

    private void showDayAppointmentsPopup(VBox owner, LocalDate day, List<Appointment> appointments) {
        ContextMenu menu = new ContextMenu();
        Label title = new Label(day + " appointments");
        title.getStyleClass().add("calendar-popup-title");
        CustomMenuItem titleItem = new CustomMenuItem(title, false);
        menu.getItems().add(titleItem);
        appointments.forEach(appointment -> {
            Label label = new Label(formatCalendarPopupItem(appointment));
            label.getStyleClass().addAll("calendar-popup-item", "calendar-badge-" + appointment.getStatus().name().toLowerCase());
            CustomMenuItem item = new CustomMenuItem(label, true);
            item.setOnAction(event -> {
                selectAppointment(appointment);
                selectAppointmentInTable(appointment);
            });
            menu.getItems().add(item);
        });
        menu.show(owner, javafx.geometry.Side.RIGHT, 8, 0);
    }

    private String formatCalendarPopupItem(Appointment appointment) {
        return appointment.getAppointmentTime().toString().substring(0, 5)
                + "  " + appointment.getPatientName()
                + "  |  " + appointment.getDoctorName()
                + "  |  " + appointment.getStatus().name();
    }

    private void selectAppointmentInTable(Appointment appointment) {
        appointmentTable.getItems().stream()
                .filter(item -> item.getId() == appointment.getId())
                .findFirst()
                .ifPresent(item -> appointmentTable.getSelectionModel().select(item));
    }

    private void configureRoleAccess() {
        if (!isDoctorSession()) {
            return;
        }
        patientCombo.setDisable(true);
        doctorCombo.setDisable(true);
        appointmentDatePicker.setDisable(true);
        timeChoice.setDisable(true);
        statusChoice.setDisable(true);
        reasonField.setDisable(true);
        notesArea.setDisable(true);
        disable(saveAppointmentButton);
        disable(clearFormButton);
        disable(checkAvailabilityButton);
        disable(openFormGoogleButton);
        disable(queueReminderButton);
        disable(sendDueRemindersButton);
        disable(completeAppointmentButton);
        disable(cancelAppointmentButton);
        setStatus("Doctor view is read-only and shows only appointments assigned to your doctor profile.", true);
    }

    private void disable(Button button) {
        if (button != null) {
            button.setDisable(true);
        }
    }

    private boolean isDoctorSession() {
        return SessionManager.getCurrentUser() != null
                && SessionManager.getCurrentUser().getRole() == Role.DOCTOR;
    }

    private void setStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-text", "error-text");
        statusLabel.getStyleClass().add(success ? "success-text" : "error-text");
    }
}
