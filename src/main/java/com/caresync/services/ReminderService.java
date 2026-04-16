package com.caresync.services;

import com.caresync.dao.ReminderQueueDAO;
import com.caresync.dao.SettingsDAO;
import com.caresync.models.Appointment;
import com.caresync.models.Gender;
import com.caresync.models.MedicalHistory;
import com.caresync.models.Patient;
import com.caresync.models.Prescription;
import com.caresync.models.PrescriptionItem;
import com.caresync.models.ReminderQueueItem;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReminderService {
    public static final String DEFAULT_REMINDER_SUBJECT = "Appointment reminder from {clinic}";
    public static final String DEFAULT_REMINDER_TEMPLATE = """
            Hello {patient},

            This is a friendly reminder from {clinic} about your upcoming appointment.

            Appointment details:
            Doctor: Dr. {doctor}
            Date: {date}
            Time: {time}

            Clinic:
            {clinic}
            {clinic_address}
            {clinic_phone}

            Please arrive 10 minutes early. If you need to reschedule, contact the clinic before your appointment time.

            Thank you,
            {clinic}
            """;

    private final ReminderQueueDAO reminderQueueDAO = new ReminderQueueDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private static final AtomicBoolean DISPATCHER_STARTED = new AtomicBoolean(false);

    public void startBackgroundDispatcher() {
        if (!DISPATCHER_STARTED.compareAndSet(false, true)) {
            return;
        }
        var executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "reminder-email-dispatcher");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                sendDueReminders();
            } catch (Exception ignored) {
                // Failed queue items are marked by sendDueReminders; startup should never fail because email is unavailable.
            }
        }, 30, 900, TimeUnit.SECONDS);
    }

    public String buildReminder(Appointment appointment) {
        try {
            return renderTemplate(settingsDAO.findAll(), appointment);
        } catch (Exception ignored) {
            // The caller can still preview a reminder if settings are temporarily unavailable.
            return renderTemplate(Map.of("reminder_template", DEFAULT_REMINDER_TEMPLATE), appointment);
        }
    }

    public void queueReminder(Appointment appointment) throws java.sql.SQLException {
        if (appointment.getPatientEmail() == null || appointment.getPatientEmail().isBlank()) {
            throw new IllegalArgumentException("Patient reminder email is missing. Add an email in Patient Management before queuing a reminder.");
        }
        Map<String, String> settings = settingsDAO.findAll();
        ReminderQueueItem item = new ReminderQueueItem();
        item.setAppointmentId(appointment.getId());
        item.setRecipientEmail(appointment.getPatientEmail().trim());
        item.setRecipientPhone(appointment.getPatientPhone());
        item.setMessage(renderTemplate(settings, appointment));
        LocalDateTime appointmentDateTime = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getAppointmentTime());
        LocalDateTime scheduledAt = appointmentDateTime.minusHours(reminderLeadHours(settings));
        item.setScheduledAt(scheduledAt.isBefore(LocalDateTime.now()) ? LocalDateTime.now() : scheduledAt);
        reminderQueueDAO.save(item);
    }

    public void sendAppointmentReminderNow(Appointment appointment) throws Exception {
        Map<String, String> settings = settingsDAO.findAll();
        if (!Boolean.parseBoolean(settings.getOrDefault("smtp_enabled", "false"))) {
            throw new IllegalStateException("SMTP is disabled. Enable email reminders in Settings before sending now.");
        }
        if (appointment.getPatientEmail() == null || appointment.getPatientEmail().isBlank()) {
            throw new IllegalArgumentException("Patient reminder email is missing. Add an email in Patient Management before sending a reminder.");
        }
        ReminderQueueItem item = new ReminderQueueItem();
        item.setAppointmentId(appointment.getId());
        item.setRecipientEmail(appointment.getPatientEmail().trim());
        item.setRecipientPhone(appointment.getPatientPhone());
        item.setMessage(renderTemplate(settings, appointment));
        sendEmail(item, settings, renderSubject(settings), null, "Appointment Reminder");
    }

    public int sendDueReminders() throws java.sql.SQLException {
        Map<String, String> settings = settingsDAO.findAll();
        if (!Boolean.parseBoolean(settings.getOrDefault("smtp_enabled", "false"))) {
            return 0;
        }
        int sent = 0;
        for (ReminderQueueItem item : reminderQueueDAO.findDuePending(LocalDateTime.now())) {
            try {
                if (sendEmail(item, settings)) {
                    reminderQueueDAO.markSent(item.getId());
                    sent++;
                } else {
                    reminderQueueDAO.markFailed(item.getId(), "Recipient email is missing.");
                }
            } catch (Exception ex) {
                reminderQueueDAO.markFailed(item.getId(), ex.getMessage());
            }
        }
        return sent;
    }

    public boolean isSmtpEnabled() throws java.sql.SQLException {
        return Boolean.parseBoolean(settingsDAO.findValue("smtp_enabled", "false"));
    }

    public void sendTestEmail(String recipientEmail, Map<String, String> settings) throws Exception {
        ReminderQueueItem item = new ReminderQueueItem();
        item.setRecipientEmail(recipientEmail);
        item.setMessage(renderTestMessage(settings));
        sendEmail(item, settings, renderSubject(settings), null, "Appointment Reminder");
    }

    public void sendPrescriptionPdf(Patient patient, MedicalHistory history, File pdfFile) throws Exception {
        Map<String, String> settings = settingsDAO.findAll();
        if (!Boolean.parseBoolean(settings.getOrDefault("smtp_enabled", "false"))) {
            throw new IllegalStateException("SMTP is disabled. Enable email reminders in Settings before sending prescription PDFs.");
        }
        if (patient.getEmail() == null || patient.getEmail().isBlank()) {
            throw new IllegalArgumentException("Patient reminder email is missing. Add an email in Patient Management before sending the prescription PDF.");
        }
        ReminderQueueItem item = new ReminderQueueItem();
        item.setRecipientEmail(patient.getEmail().trim());
        item.setMessage(renderPrescriptionMessage(settings, patient, history));
        sendEmail(item, settings, renderPrescriptionSubject(settings), pdfFile, "Medical Report");
    }

    public void sendTestPrescriptionEmail(String recipientEmail, Map<String, String> settings) throws Exception {
        Patient patient = buildTestPatient(recipientEmail);
        MedicalHistory history = buildTestHistory(patient);
        Prescription prescription = buildTestPrescription();
        File pdfFile = new PdfService().generatePrescription(patient, history, prescription, settings);

        ReminderQueueItem item = new ReminderQueueItem();
        item.setRecipientEmail(recipientEmail);
        item.setMessage(renderPrescriptionMessage(settings, patient, history)
                + "\n\nThis is a CareSync SMTP test with a sample medical report PDF attached.");
        sendEmail(item, settings, "Test " + renderPrescriptionSubject(settings), pdfFile, "Medical Report");
    }

    private boolean sendEmail(ReminderQueueItem item, Map<String, String> settings) throws Exception {
        return sendEmail(item, settings, renderSubject(settings), null, "Appointment Reminder");
    }

    private boolean sendEmail(ReminderQueueItem item, Map<String, String> settings, String subject,
                              File attachment, String emailType) throws Exception {
        if (item.getRecipientEmail() == null
                || item.getRecipientEmail().isBlank()) {
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", settings.getOrDefault("smtp_tls_enabled", "true"));
        props.put("mail.smtp.host", settings.getOrDefault("smtp_host", ""));
        props.put("mail.smtp.port", settings.getOrDefault("smtp_port", "587"));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.smtp.ssl.trust", settings.getOrDefault("smtp_host", "smtp.gmail.com"));

        String username = settings.getOrDefault("smtp_username", "");
        String password = settings.getOrDefault("smtp_password", "");
        jakarta.mail.Session session = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(username, password);
            }
        });

        jakarta.mail.Message message = new jakarta.mail.internet.MimeMessage(session);
        String fromAddress = settings.getOrDefault("smtp_from", username);
        String fromName = settings.getOrDefault("smtp_from_name", "CareSync");
        message.setFrom(new jakarta.mail.internet.InternetAddress(fromAddress, fromName));
        message.setRecipients(jakarta.mail.Message.RecipientType.TO,
                jakarta.mail.internet.InternetAddress.parse(item.getRecipientEmail()));
        message.setSubject(subject);
        message.setContent(buildEmailContent(item.getMessage(), settings, attachment, emailType));
        jakarta.mail.Transport.send(message);
        return true;
    }

    private String renderTemplate(Map<String, String> settings, Appointment appointment) {
        String template = settings.getOrDefault("reminder_template", DEFAULT_REMINDER_TEMPLATE);
        return template
                .replace("{patient}", safe(appointment.getPatientName()))
                .replace("{doctor}", safeDoctor(appointment.getDoctorName()))
                .replace("{date}", String.valueOf(appointment.getAppointmentDate()))
                .replace("{time}", String.valueOf(appointment.getAppointmentTime()))
                .replace("{clinic}", settings.getOrDefault("clinic_name", "CareSync Clinic"))
                .replace("{clinic_phone}", settings.getOrDefault("clinic_phone", ""))
                .replace("{clinic_address}", settings.getOrDefault("clinic_address", ""));
    }

    private String renderSubject(Map<String, String> settings) {
        return settings.getOrDefault("reminder_subject", DEFAULT_REMINDER_SUBJECT)
                .replace("{clinic}", settings.getOrDefault("clinic_name", "CareSync Clinic"))
                .replace("{clinic_phone}", settings.getOrDefault("clinic_phone", ""))
                .replace("{clinic_address}", settings.getOrDefault("clinic_address", ""));
    }

    private String renderTestMessage(Map<String, String> settings) {
        String template = settings.getOrDefault("reminder_template", DEFAULT_REMINDER_TEMPLATE);
        return template
                .replace("{patient}", "Test Patient")
                .replace("{doctor}", "CareSync Demo Doctor")
                .replace("{date}", String.valueOf(LocalDate.now().plusDays(1)))
                .replace("{time}", "09:00")
                .replace("{clinic}", settings.getOrDefault("clinic_name", "CareSync Clinic"))
                .replace("{clinic_phone}", settings.getOrDefault("clinic_phone", ""))
                .replace("{clinic_address}", settings.getOrDefault("clinic_address", ""));
    }

    private String renderPrescriptionSubject(Map<String, String> settings) {
        return "Medical report from " + settings.getOrDefault("clinic_name", "CareSync Clinic");
    }

    private String renderPrescriptionMessage(Map<String, String> settings, Patient patient, MedicalHistory history) {
        return """
                Hello {patient},

                Your medical report and prescription from {clinic} is attached as a PDF.

                Report details:
                Doctor: Dr. {doctor}
                Diagnosis: {diagnosis}
                Date: {date}

                Please follow the instructions in the attached document. If you have questions or need clarification, contact the clinic.

                {clinic}
                {clinic_address}
                {clinic_phone}
                """.replace("{patient}", safe(patient.getFullName()))
                .replace("{doctor}", safeDoctor(history.getDoctorName()))
                .replace("{diagnosis}", safe(history.getDiagnosis()))
                .replace("{date}", String.valueOf(history.getCreatedAt() == null ? LocalDate.now() : history.getCreatedAt().toLocalDate()))
                .replace("{clinic}", settings.getOrDefault("clinic_name", "CareSync Clinic"))
                .replace("{clinic_phone}", settings.getOrDefault("clinic_phone", ""))
                .replace("{clinic_address}", settings.getOrDefault("clinic_address", ""));
    }

    private int reminderLeadHours(Map<String, String> settings) {
        try {
            return Math.max(1, Integer.parseInt(settings.getOrDefault("reminder_lead_hours", "24").trim()));
        } catch (NumberFormatException ex) {
            return 24;
        }
    }

    private jakarta.mail.Multipart buildEmailContent(String message, Map<String, String> settings,
                                                     File attachment, String emailType) throws Exception {
        jakarta.mail.internet.MimeMultipart multipart = new jakarta.mail.internet.MimeMultipart("alternative");

        jakarta.mail.internet.MimeBodyPart textPart = new jakarta.mail.internet.MimeBodyPart();
        textPart.setText(message, "UTF-8");

        jakarta.mail.internet.MimeBodyPart htmlPart = new jakarta.mail.internet.MimeBodyPart();
        htmlPart.setContent(buildHtmlReminder(message, settings, emailType), "text/html; charset=UTF-8");

        multipart.addBodyPart(textPart);
        multipart.addBodyPart(htmlPart);

        if (attachment == null) {
            return multipart;
        }

        jakarta.mail.internet.MimeMultipart mixed = new jakarta.mail.internet.MimeMultipart("mixed");
        jakarta.mail.internet.MimeBodyPart bodyPart = new jakarta.mail.internet.MimeBodyPart();
        bodyPart.setContent(multipart);
        mixed.addBodyPart(bodyPart);

        jakarta.mail.internet.MimeBodyPart attachmentPart = new jakarta.mail.internet.MimeBodyPart();
        attachmentPart.attachFile(attachment);
        attachmentPart.setFileName(attachment.getName());
        mixed.addBodyPart(attachmentPart);
        return mixed;
    }

    private String buildHtmlReminder(String message, Map<String, String> settings, String emailType) {
        String clinic = escapeHtml(settings.getOrDefault("clinic_name", "CareSync Clinic"));
        String body = escapeHtml(message).replace("\n", "<br>");
        String type = escapeHtml(emailType == null || emailType.isBlank() ? "CareSync Message" : emailType);
        return """
                <!doctype html>
                <html>
                <body style="margin:0;padding:0;background:#eef5f4;font-family:Segoe UI,Arial,sans-serif;color:#172033;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#eef5f4;padding:28px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="640" cellpadding="0" cellspacing="0" style="max-width:640px;background:#ffffff;border:1px solid #dbe8ec;border-radius:12px;overflow:hidden;">
                          <tr>
                            <td style="background:#0f7580;background:linear-gradient(135deg,#0f7580,#1d2c50);padding:26px 30px;color:#ffffff;">
                              <div style="font-size:12px;font-weight:800;letter-spacing:.08em;text-transform:uppercase;color:#d7f4f1;">%s</div>
                              <div style="font-size:28px;font-weight:900;margin-top:6px;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px 30px;font-size:15px;line-height:1.65;">
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 30px;background:#f7fbfb;border-top:1px solid #dbe8ec;color:#617086;font-size:12px;">
                              Sent by CareSync. Please do not reply to this automated reminder unless your clinic uses this inbox for scheduling.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(type, clinic, body);
    }

    private Patient buildTestPatient(String recipientEmail) {
        Patient patient = new Patient();
        patient.setId(0);
        patient.setFirstName("Test");
        patient.setLastName("Patient");
        patient.setGender(Gender.FEMALE);
        patient.setDateOfBirth(LocalDate.now().minusYears(34));
        patient.setPhone("+1 555 0100");
        patient.setEmail(recipientEmail);
        patient.setAddress("CareSync sample address");
        patient.setBloodGroup("O+");
        patient.setAllergies("No known allergies");
        patient.setEmergencyContact("CareSync Test Contact");
        return patient;
    }

    private MedicalHistory buildTestHistory(Patient patient) {
        MedicalHistory history = new MedicalHistory();
        history.setId(0);
        history.setPatientId(patient.getId());
        history.setDoctorName("CareSync Demo Doctor");
        history.setDiagnosis("Sample follow-up consultation");
        history.setTreatmentNotes("This is a sample medical report PDF generated only to test SMTP attachments.");
        history.setCreatedAt(LocalDateTime.now());
        return history;
    }

    private Prescription buildTestPrescription() {
        Prescription prescription = new Prescription();
        prescription.setInstructions("Take medication as directed. This sample PDF is not a real prescription.");
        PrescriptionItem item = new PrescriptionItem();
        item.setMedicineName("Sample Medicine");
        item.setDosage("1 tablet");
        item.setFrequency("Twice daily");
        item.setDuration("5 days");
        item.setNotes("SMTP attachment test only");
        prescription.getItems().add(item);
        return prescription;
    }

    private String escapeHtml(String value) {
        String safeValue = value == null ? "" : value;
        return safeValue
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Patient" : value.trim();
    }

    private String safeDoctor(String value) {
        return safe(value).replaceFirst("(?i)^dr\\.\\s*", "").strip();
    }
}
