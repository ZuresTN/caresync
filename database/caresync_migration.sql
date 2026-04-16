USE caresync;

ALTER TABLE users MODIFY role ENUM('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT') NOT NULL;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE users ADD COLUMN password_must_change BOOLEAN NOT NULL DEFAULT FALSE AFTER active',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'password_must_change'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'updated_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE patients ADD COLUMN user_id INT NULL AFTER id',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'patients' AND COLUMN_NAME = 'user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE patients
SET gender = UPPER(gender)
WHERE gender IN ('Male', 'Female', 'male', 'female');

UPDATE patients
SET gender = NULL
WHERE gender IS NOT NULL AND gender NOT IN ('MALE', 'FEMALE');

ALTER TABLE patients MODIFY gender ENUM('MALE', 'FEMALE') NULL;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE patients ADD UNIQUE KEY idx_patients_user_id (user_id)',
        'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'patients' AND INDEX_NAME = 'idx_patients_user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE patients ADD CONSTRAINT fk_patients_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL',
        'SELECT 1')
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'patients' AND CONSTRAINT_NAME = 'fk_patients_user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE appointments MODIFY status ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED', 'PENDING', 'CONFIRMED') NOT NULL DEFAULT 'SCHEDULED';
UPDATE appointments SET status = 'SCHEDULED' WHERE status = 'CONFIRMED';
ALTER TABLE appointments MODIFY status ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED', 'PENDING') NOT NULL DEFAULT 'SCHEDULED';

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE appointments ADD INDEX idx_appointment_conflict (doctor_id, appointment_date, appointment_time, status)',
        'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointments' AND INDEX_NAME = 'idx_appointment_conflict'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical_records')
        AND NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical_history'),
        'RENAME TABLE medical_records TO medical_history',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
);

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
);

CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(80) PRIMARY KEY,
    setting_value TEXT NOT NULL
);

ALTER TABLE system_settings MODIFY setting_value TEXT NOT NULL;

INSERT INTO system_settings (setting_key, setting_value) VALUES
('reminder_subject', 'Appointment reminder from {clinic}'),
('reminder_lead_hours', '24'),
('reminder_template', 'Hello {patient},\n\nThis is a friendly reminder from {clinic} about your upcoming appointment.\n\nAppointment details:\nDoctor: Dr. {doctor}\nDate: {date}\nTime: {time}\n\nClinic:\n{clinic}\n{clinic_address}\n{clinic_phone}\n\nPlease arrive 10 minutes early. If you need to reschedule, contact the clinic before your appointment time.\n\nThank you,\n{clinic}'),
('smtp_enabled', 'false'),
('smtp_host', ''),
('smtp_port', '587'),
('smtp_username', ''),
('smtp_password', ''),
('smtp_from', ''),
('smtp_from_name', 'CareSync'),
('smtp_tls_enabled', 'true')
ON DUPLICATE KEY UPDATE setting_value = setting_value;

SET @fk := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'prescriptions'
      AND COLUMN_NAME = 'medical_record_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk IS NULL, 'SELECT 1', CONCAT('ALTER TABLE prescriptions DROP FOREIGN KEY `', @fk, '`'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE prescriptions CHANGE medical_record_id medical_history_id INT NOT NULL',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'prescriptions' AND COLUMN_NAME = 'medical_record_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE prescriptions ADD CONSTRAINT fk_prescriptions_medical_history FOREIGN KEY (medical_history_id) REFERENCES medical_history(id) ON DELETE CASCADE',
        'SELECT 1')
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'prescriptions'
      AND CONSTRAINT_NAME = 'fk_prescriptions_medical_history'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
