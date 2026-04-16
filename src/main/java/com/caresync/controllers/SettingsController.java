package com.caresync.controllers;

import com.caresync.dao.SettingsDAO;
import com.caresync.services.AuditService;
import com.caresync.services.ReminderService;
import com.caresync.services.ValidationService;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.Map;

public class SettingsController {
    @FXML private TextField clinicNameField;
    @FXML private TextField clinicPhoneField;
    @FXML private TextField clinicAddressField;
    @FXML private TextField reminderSubjectField;
    @FXML private TextField reminderLeadHoursField;
    @FXML private TextArea reminderTemplateArea;
    @FXML private CheckBox smtpEnabledCheck;
    @FXML private TextField smtpHostField;
    @FXML private TextField smtpPortField;
    @FXML private TextField smtpUsernameField;
    @FXML private PasswordField smtpPasswordField;
    @FXML private TextField smtpFromField;
    @FXML private TextField smtpFromNameField;
    @FXML private TextField smtpTestRecipientField;
    @FXML private CheckBox smtpTlsCheck;
    @FXML private Label statusLabel;

    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final ValidationService validationService = new ValidationService();
    private final AuditService auditService = new AuditService();
    private final ReminderService reminderService = new ReminderService();

    @FXML
    private void initialize() {
        loadSettings();
    }

    @FXML
    private void saveSettings() {
        try {
            validationService.requireText(clinicNameField.getText(), "Clinic name");
            validationService.requireText(reminderSubjectField.getText(), "Reminder subject");
            validationService.requireText(reminderTemplateArea.getText(), "Reminder template");
            validationService.validatePhone(clinicPhoneField.getText(), "Clinic phone", false);
            if (smtpEnabledCheck.isSelected()) {
                validationService.requireText(smtpHostField.getText(), "SMTP host");
                validationService.requireText(smtpUsernameField.getText(), "SMTP username");
                validationService.requireText(smtpPasswordField.getText(), "SMTP app password");
                validationService.validateEmail(smtpFromField.getText(), "SMTP from address", true);
            } else {
                validationService.validateEmail(smtpFromField.getText(), "SMTP from address", false);
            }
            int smtpPort = Integer.parseInt(smtpPortField.getText().trim());
            int leadHours = Integer.parseInt(reminderLeadHoursField.getText().trim());
            if (smtpPort <= 0 || leadHours <= 0) {
                throw new IllegalArgumentException("SMTP port and reminder lead hours must be positive numbers.");
            }
            settingsDAO.save("clinic_name", clinicNameField.getText().trim());
            settingsDAO.save("clinic_phone", clinicPhoneField.getText().trim());
            settingsDAO.save("clinic_address", clinicAddressField.getText().trim());
            settingsDAO.save("reminder_subject", reminderSubjectField.getText().trim());
            settingsDAO.save("reminder_lead_hours", reminderLeadHoursField.getText().trim());
            settingsDAO.save("reminder_template", reminderTemplateArea.getText().trim());
            settingsDAO.save("smtp_enabled", String.valueOf(smtpEnabledCheck.isSelected()));
            settingsDAO.save("smtp_host", smtpHostField.getText().trim());
            settingsDAO.save("smtp_port", smtpPortField.getText().trim());
            settingsDAO.save("smtp_username", smtpUsernameField.getText().trim());
            settingsDAO.save("smtp_password", smtpPasswordField.getText());
            settingsDAO.save("smtp_from", smtpFromField.getText().trim());
            settingsDAO.save("smtp_from_name", smtpFromNameField.getText().trim());
            settingsDAO.save("smtp_tls_enabled", String.valueOf(smtpTlsCheck.isSelected()));
            auditService.record("UPDATE", "SETTINGS", null, "System settings updated.");
            setStatus("Settings saved.", true);
        } catch (Exception ex) {
            setStatus("Could not save settings: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void applyGmailDefaults() {
        smtpEnabledCheck.setSelected(true);
        smtpHostField.setText("smtp.gmail.com");
        smtpPortField.setText("587");
        smtpTlsCheck.setSelected(true);
        if (smtpFromField.getText().isBlank() && !smtpUsernameField.getText().isBlank()) {
            smtpFromField.setText(smtpUsernameField.getText().trim());
        }
        if (smtpFromNameField.getText().isBlank()) {
            smtpFromNameField.setText("CareSync");
        }
        setStatus("Gmail SMTP defaults applied. Use your Gmail address as username and a Google app password, not your normal Gmail password.", true);
    }

    @FXML
    private void useDefaultReminderTemplate() {
        reminderSubjectField.setText(ReminderService.DEFAULT_REMINDER_SUBJECT);
        reminderLeadHoursField.setText("24");
        reminderTemplateArea.setText(ReminderService.DEFAULT_REMINDER_TEMPLATE.trim());
        setStatus("Appointment reminder template applied.", true);
    }

    @FXML
    private void sendTestEmail() {
        try {
            validationService.validateEmail(smtpTestRecipientField.getText(), "Test recipient", true);
            Map<String, String> settings = collectSettingsFromForm();
            validateSmtpSettings(settings);
            reminderService.sendTestEmail(smtpTestRecipientField.getText().trim(), settings);
            setStatus("Test email sent to " + smtpTestRecipientField.getText().trim() + ".", true);
        } catch (Exception ex) {
            setStatus("Could not send test email: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void sendTestPrescriptionEmail() {
        try {
            validationService.validateEmail(smtpTestRecipientField.getText(), "Test recipient", true);
            Map<String, String> settings = collectSettingsFromForm();
            validateSmtpSettings(settings);
            reminderService.sendTestPrescriptionEmail(smtpTestRecipientField.getText().trim(), settings);
            setStatus("Test prescription PDF sent to " + smtpTestRecipientField.getText().trim() + ".", true);
        } catch (Exception ex) {
            setStatus("Could not send test prescription PDF: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void sendShowcaseReminderEmail() {
        try {
            validationService.validateEmail(smtpTestRecipientField.getText(), "Showcase recipient", true);
            Map<String, String> settings = collectSettingsFromForm();
            validateSmtpSettings(settings);
            reminderService.sendTestEmail(smtpTestRecipientField.getText().trim(), settings);
            setStatus("Showcase reminder email sent to " + smtpTestRecipientField.getText().trim() + ".", true);
        } catch (Exception ex) {
            setStatus("Could not send showcase reminder: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void sendShowcasePrescriptionEmail() {
        try {
            validationService.validateEmail(smtpTestRecipientField.getText(), "Showcase recipient", true);
            Map<String, String> settings = collectSettingsFromForm();
            validateSmtpSettings(settings);
            reminderService.sendTestPrescriptionEmail(smtpTestRecipientField.getText().trim(), settings);
            setStatus("Showcase prescription PDF sent to " + smtpTestRecipientField.getText().trim() + ".", true);
        } catch (Exception ex) {
            setStatus("Could not send showcase prescription PDF: " + ex.getMessage(), false);
        }
    }

    private void loadSettings() {
        try {
            Map<String, String> settings = settingsDAO.findAll();
            clinicNameField.setText(settings.getOrDefault("clinic_name", "CareSync Clinic"));
            clinicPhoneField.setText(settings.getOrDefault("clinic_phone", ""));
            clinicAddressField.setText(settings.getOrDefault("clinic_address", ""));
            reminderSubjectField.setText(settings.getOrDefault("reminder_subject", ReminderService.DEFAULT_REMINDER_SUBJECT));
            reminderLeadHoursField.setText(settings.getOrDefault("reminder_lead_hours", "24"));
            reminderTemplateArea.setText(settings.getOrDefault("reminder_template", ReminderService.DEFAULT_REMINDER_TEMPLATE).trim());
            smtpEnabledCheck.setSelected(Boolean.parseBoolean(settings.getOrDefault("smtp_enabled", "false")));
            smtpHostField.setText(settings.getOrDefault("smtp_host", ""));
            smtpPortField.setText(settings.getOrDefault("smtp_port", "587"));
            smtpUsernameField.setText(settings.getOrDefault("smtp_username", ""));
            smtpPasswordField.setText(settings.getOrDefault("smtp_password", ""));
            smtpFromField.setText(settings.getOrDefault("smtp_from", ""));
            smtpFromNameField.setText(settings.getOrDefault("smtp_from_name", "CareSync"));
            smtpTlsCheck.setSelected(Boolean.parseBoolean(settings.getOrDefault("smtp_tls_enabled", "true")));
        } catch (Exception ex) {
            setStatus("Could not load settings: " + ex.getMessage(), false);
        }
    }

    private Map<String, String> collectSettingsFromForm() {
        return Map.ofEntries(
                Map.entry("clinic_name", clinicNameField.getText().trim()),
                Map.entry("clinic_phone", clinicPhoneField.getText().trim()),
                Map.entry("clinic_address", clinicAddressField.getText().trim()),
                Map.entry("reminder_subject", reminderSubjectField.getText().trim()),
                Map.entry("reminder_template", reminderTemplateArea.getText().trim()),
                Map.entry("smtp_enabled", String.valueOf(smtpEnabledCheck.isSelected())),
                Map.entry("smtp_host", smtpHostField.getText().trim()),
                Map.entry("smtp_port", smtpPortField.getText().trim()),
                Map.entry("smtp_username", smtpUsernameField.getText().trim()),
                Map.entry("smtp_password", smtpPasswordField.getText()),
                Map.entry("smtp_from", smtpFromField.getText().trim()),
                Map.entry("smtp_from_name", smtpFromNameField.getText().trim().isEmpty() ? "CareSync" : smtpFromNameField.getText().trim()),
                Map.entry("smtp_tls_enabled", String.valueOf(smtpTlsCheck.isSelected()))
        );
    }

    private void validateSmtpSettings(Map<String, String> settings) {
        validationService.requireText(settings.get("smtp_host"), "SMTP host");
        validationService.requireText(settings.get("smtp_username"), "SMTP username");
        validationService.requireText(settings.get("smtp_password"), "SMTP app password");
        validationService.validateEmail(settings.get("smtp_from"), "SMTP from address", true);
        int smtpPort = Integer.parseInt(settings.get("smtp_port"));
        if (smtpPort <= 0) {
            throw new IllegalArgumentException("SMTP port must be a positive number.");
        }
    }

    private void setStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-text", "error-text");
        statusLabel.getStyleClass().add(success ? "success-text" : "error-text");
    }
}
