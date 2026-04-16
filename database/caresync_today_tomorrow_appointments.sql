USE caresync;

START TRANSACTION;

-- Fake appointments for today and tomorrow only.
-- Safe to rerun: it removes only appointments with the CS-TODAY-TOMORROW-DEMO marker.
-- Requires at least one doctor and at least one patient to already exist.

DELETE rq
FROM reminder_queue rq
JOIN appointments a ON a.id = rq.appointment_id
WHERE a.notes LIKE '%CS-TODAY-TOMORROW-DEMO%';

DELETE FROM appointments
WHERE notes LIKE '%CS-TODAY-TOMORROW-DEMO%';

SET @doctor_1_id = (SELECT id FROM doctors ORDER BY id LIMIT 1);
SET @doctor_2_id = (
    SELECT COALESCE(
        (SELECT id FROM doctors WHERE id <> @doctor_1_id ORDER BY id LIMIT 1),
        @doctor_1_id
    )
);

SET @patient_1_id = (SELECT id FROM patients ORDER BY id LIMIT 1);
SET @patient_2_id = (
    SELECT COALESCE(
        (SELECT id FROM patients WHERE id <> @patient_1_id ORDER BY id LIMIT 1),
        @patient_1_id
    )
);
SET @patient_3_id = (
    SELECT COALESCE(
        (SELECT id FROM patients WHERE id NOT IN (@patient_1_id, @patient_2_id) ORDER BY id LIMIT 1),
        @patient_1_id
    )
);
SET @patient_4_id = (
    SELECT COALESCE(
        (SELECT id FROM patients WHERE id NOT IN (@patient_1_id, @patient_2_id, @patient_3_id) ORDER BY id LIMIT 1),
        @patient_2_id
    )
);
SET @patient_5_id = (
    SELECT COALESCE(
        (SELECT id FROM patients WHERE id NOT IN (@patient_1_id, @patient_2_id, @patient_3_id, @patient_4_id) ORDER BY id LIMIT 1),
        @patient_3_id
    )
);
SET @patient_6_id = (
    SELECT COALESCE(
        (SELECT id FROM patients WHERE id NOT IN (@patient_1_id, @patient_2_id, @patient_3_id, @patient_4_id, @patient_5_id) ORDER BY id LIMIT 1),
        @patient_4_id
    )
);

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_1_id, @doctor_1_id, CURDATE(), '09:00:00', 'SCHEDULED',
       'Blood pressure follow-up',
       'CS-TODAY-TOMORROW-DEMO | Bring recent readings and medication list.'
WHERE @doctor_1_id IS NOT NULL AND @patient_1_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_2_id, @doctor_2_id, CURDATE(), '10:00:00', 'PENDING',
       'New patient consultation',
       'CS-TODAY-TOMORROW-DEMO | Confirm contact details at check-in.'
WHERE @doctor_2_id IS NOT NULL AND @patient_2_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_3_id, @doctor_1_id, CURDATE(), '11:30:00', 'SCHEDULED',
       'Medication review',
       'CS-TODAY-TOMORROW-DEMO | Review current prescription tolerance.'
WHERE @doctor_1_id IS NOT NULL AND @patient_3_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_4_id, @doctor_2_id, CURDATE(), '14:00:00', 'SCHEDULED',
       'Allergy symptoms',
       'CS-TODAY-TOMORROW-DEMO | Patient reports seasonal flare-up.'
WHERE @doctor_2_id IS NOT NULL AND @patient_4_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_5_id, @doctor_1_id, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:30:00', 'SCHEDULED',
       'Diabetes care review',
       'CS-TODAY-TOMORROW-DEMO | Ask patient to bring glucose diary.'
WHERE @doctor_1_id IS NOT NULL AND @patient_5_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_6_id, @doctor_2_id, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:30:00', 'PENDING',
       'Lab results discussion',
       'CS-TODAY-TOMORROW-DEMO | Lab report should be reviewed before visit.'
WHERE @doctor_2_id IS NOT NULL AND @patient_6_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_1_id, @doctor_1_id, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:00:00', 'SCHEDULED',
       'Routine follow-up',
       'CS-TODAY-TOMORROW-DEMO | Follow-up after recent completed appointment.'
WHERE @doctor_1_id IS NOT NULL AND @patient_1_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_2_id, @doctor_2_id, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '15:00:00', 'SCHEDULED',
       'Wellness check',
       'CS-TODAY-TOMORROW-DEMO | General wellness and vitals review.'
WHERE @doctor_2_id IS NOT NULL AND @patient_2_id IS NOT NULL;

INSERT INTO reminder_queue(appointment_id, recipient_email, recipient_phone, channel, status, message, scheduled_at)
SELECT a.id, p.email, p.phone, 'EMAIL', 'PENDING',
       CONCAT('Hello ', p.first_name, ', your appointment is on ', a.appointment_date, ' at ', TIME_FORMAT(a.appointment_time, '%H:%i'), '.'),
       DATE_SUB(CONCAT(a.appointment_date, ' ', a.appointment_time), INTERVAL 1 DAY)
FROM appointments a
JOIN patients p ON p.id = a.patient_id
WHERE a.notes LIKE '%CS-TODAY-TOMORROW-DEMO%'
  AND a.status IN ('SCHEDULED', 'PENDING');

INSERT INTO audit_logs(actor_user_id, action, entity_type, entity_id, summary) VALUES
(NULL, 'CREATE', 'APPOINTMENT', NULL, 'Fake appointments generated for today and tomorrow.');

COMMIT;
