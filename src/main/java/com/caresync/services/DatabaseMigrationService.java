package com.caresync.services;

import com.caresync.utils.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseMigrationService {
    public void ensureCurrentSchema() throws SQLException {
        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : schemaStatements()) {
                statement.execute(sql);
            }
            statement.execute("ALTER TABLE system_settings MODIFY setting_value TEXT NOT NULL");
            addColumnIfMissing(connection, statement, "users", "password_must_change",
                    "ALTER TABLE users ADD COLUMN password_must_change BOOLEAN NOT NULL DEFAULT FALSE AFTER active");
            addColumnIfMissing(connection, statement, "users", "updated_at",
                    "ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at");
            addColumnIfMissing(connection, statement, "patients", "user_id",
                    "ALTER TABLE patients ADD COLUMN user_id INT NULL AFTER id");
            clearDuplicatePatientUserLinks(statement);
            if (!indexExists(connection, "patients", "idx_patients_user_id")) {
                statement.execute("ALTER TABLE patients ADD UNIQUE KEY idx_patients_user_id (user_id)");
            }
            addForeignKeyIfMissing(connection, statement, "patients", "user_id", "users", "id",
                    "ALTER TABLE patients ADD CONSTRAINT fk_patients_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL");
            addIndexIfMissing(connection, statement, "appointments", "idx_appointment_conflict",
                    "ALTER TABLE appointments ADD INDEX idx_appointment_conflict (doctor_id, appointment_date, appointment_time, status)");
            statement.execute("""
                    INSERT INTO system_settings (setting_key, setting_value) VALUES
                    ('clinic_name', 'CareSync Clinic'),
                    ('clinic_phone', '+1 555 0100'),
                    ('clinic_address', '100 Wellness Avenue'),
                    ('reminder_subject', 'Appointment reminder from {clinic}'),
                    ('reminder_lead_hours', '24'),
                    ('reminder_template', 'Hello {patient},\\n\\nThis is a friendly reminder from {clinic} about your upcoming appointment.\\n\\nAppointment details:\\nDoctor: Dr. {doctor}\\nDate: {date}\\nTime: {time}\\n\\nClinic:\\n{clinic}\\n{clinic_address}\\n{clinic_phone}\\n\\nPlease arrive 10 minutes early. If you need to reschedule, contact the clinic before your appointment time.\\n\\nThank you,\\n{clinic}'),
                    ('smtp_enabled', 'false'),
                    ('smtp_host', ''),
                    ('smtp_port', '587'),
                    ('smtp_username', ''),
                    ('smtp_password', ''),
                    ('smtp_from', ''),
                    ('smtp_from_name', 'CareSync'),
                    ('smtp_tls_enabled', 'true')
                    ON DUPLICATE KEY UPDATE setting_value = setting_value
                    """);
        }
    }

    private void addColumnIfMissing(Connection connection, Statement statement, String table, String column, String sql) throws SQLException {
        if (!columnExists(connection, table, column)) {
            statement.execute(sql);
        }
    }

    private void addIndexIfMissing(Connection connection, Statement statement, String table, String indexOrColumn, String sql) throws SQLException {
        if (!indexExists(connection, table, indexOrColumn) && !singleColumnIndexExists(connection, table, indexOrColumn)) {
            statement.execute(sql);
        }
    }

    private void addForeignKeyIfMissing(Connection connection, Statement statement, String table, String column,
                                        String referencedTable, String referencedColumn, String sql) throws SQLException {
        if (!foreignKeyExists(connection, table, column, referencedTable, referencedColumn)) {
            statement.execute(sql);
        }
    }

    private void clearDuplicatePatientUserLinks(Statement statement) throws SQLException {
        statement.execute("""
                UPDATE patients p
                JOIN (
                    SELECT id, user_id
                    FROM (
                        SELECT id, user_id,
                               ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY id) AS rn
                        FROM patients
                        WHERE user_id IS NOT NULL
                    ) ranked
                    WHERE rn > 1
                ) duplicates ON duplicates.id = p.id
                SET p.user_id = NULL
                """);
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean indexExists(Connection connection, String table, String index) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, index);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean singleColumnIndexExists(Connection connection, String table, String column) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                  AND SEQ_IN_INDEX = 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean foreignKeyExists(Connection connection, String table, String column,
                                     String referencedTable, String referencedColumn) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                  AND REFERENCED_TABLE_NAME = ?
                  AND REFERENCED_COLUMN_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            statement.setString(3, referencedTable);
            statement.setString(4, referencedColumn);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private String[] schemaStatements() {
        return new String[]{
                """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    full_name VARCHAR(120) NOT NULL,
                    username VARCHAR(80) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    role ENUM('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT') NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    password_must_change BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS doctors (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL UNIQUE,
                    specialization VARCHAR(100) NOT NULL,
                    phone VARCHAR(40),
                    email VARCHAR(120),
                    room VARCHAR(40),
                    availability VARCHAR(255),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS receptionists (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL UNIQUE,
                    phone VARCHAR(40),
                    email VARCHAR(120),
                    shift VARCHAR(80),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS patients (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NULL UNIQUE,
                    first_name VARCHAR(80) NOT NULL,
                    last_name VARCHAR(80) NOT NULL,
                    gender ENUM('MALE', 'FEMALE'),
                    date_of_birth DATE,
                    phone VARCHAR(40),
                    email VARCHAR(120),
                    address VARCHAR(255),
                    blood_group VARCHAR(10),
                    allergies VARCHAR(255),
                    emergency_contact VARCHAR(120),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_patients_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS appointments (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    patient_id INT NOT NULL,
                    doctor_id INT NOT NULL,
                    appointment_date DATE NOT NULL,
                    appointment_time TIME NOT NULL,
                    status ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED', 'PENDING') NOT NULL DEFAULT 'SCHEDULED',
                    reason VARCHAR(255),
                    notes TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
                    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
                    INDEX idx_appointment_date (appointment_date),
                    INDEX idx_appointment_doctor (doctor_id),
                    INDEX idx_appointment_conflict (doctor_id, appointment_date, appointment_time, status)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS medical_history (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    patient_id INT NOT NULL,
                    doctor_id INT NOT NULL,
                    appointment_id INT,
                    diagnosis VARCHAR(255) NOT NULL,
                    treatment_notes TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
                    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
                    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS prescriptions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    medical_history_id INT NOT NULL,
                    instructions TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_prescriptions_medical_history FOREIGN KEY (medical_history_id) REFERENCES medical_history(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS prescription_items (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    prescription_id INT NOT NULL,
                    medicine_name VARCHAR(120) NOT NULL,
                    dosage VARCHAR(80) NOT NULL,
                    frequency VARCHAR(80),
                    duration VARCHAR(80),
                    notes VARCHAR(255),
                    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE
                )
                """,
                "CREATE TABLE IF NOT EXISTS system_settings (setting_key VARCHAR(80) PRIMARY KEY, setting_value TEXT NOT NULL)",
                """
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    actor_user_id INT NULL,
                    action VARCHAR(80) NOT NULL,
                    entity_type VARCHAR(80) NOT NULL,
                    entity_id INT NULL,
                    summary VARCHAR(255),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
                    INDEX idx_audit_action (action),
                    INDEX idx_audit_entity (entity_type, entity_id),
                    INDEX idx_audit_created_at (created_at)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS reminder_queue (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    appointment_id INT NOT NULL,
                    recipient_email VARCHAR(120),
                    recipient_phone VARCHAR(40),
                    channel ENUM('EMAIL') NOT NULL DEFAULT 'EMAIL',
                    status ENUM('PENDING', 'SENT', 'FAILED') NOT NULL DEFAULT 'PENDING',
                    message TEXT NOT NULL,
                    error_message VARCHAR(255),
                    scheduled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    sent_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
                    INDEX idx_reminder_status_time (status, scheduled_at)
                )
                """
        };
    }
}
