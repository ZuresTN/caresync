USE caresync;

START TRANSACTION;

-- CareSync fake clinical data only.
-- This file does NOT create login accounts, users, doctors, or receptionists.
-- It uses existing doctors from your clinic database, then adds fake patients,
-- appointments, medical history, prescriptions, reminders, and audit rows.

SET @doctor_primary_id = (SELECT id FROM doctors ORDER BY id LIMIT 1);
SET @doctor_secondary_id = (
    SELECT COALESCE(
        (SELECT id FROM doctors WHERE id <> @doctor_primary_id ORDER BY id LIMIT 1),
        @doctor_primary_id
    )
);

-- If there are no doctors yet, create at least one real doctor in the app first.
INSERT INTO patients(user_id, first_name, last_name, gender, date_of_birth, phone, email, address, blood_group, allergies, emergency_contact)
SELECT NULL, 'Lina', 'Patel', 'FEMALE', '1991-04-12', '+1 555 4101', 'lina.patel@example.test',
       '45 Maple Street', 'O+', 'Penicillin', 'Arjun Patel +1 555 8101'
WHERE @doctor_primary_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM patients WHERE email = 'lina.patel@example.test');

INSERT INTO patients(user_id, first_name, last_name, gender, date_of_birth, phone, email, address, blood_group, allergies, emergency_contact)
SELECT NULL, 'Sam', 'Williams', 'MALE', '1984-09-22', '+1 555 4102', 'sam.williams@example.test',
       '78 Cedar Avenue', 'A-', 'None reported', 'Nora Williams +1 555 8102'
WHERE @doctor_primary_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM patients WHERE email = 'sam.williams@example.test');

INSERT INTO patients(user_id, first_name, last_name, gender, date_of_birth, phone, email, address, blood_group, allergies, emergency_contact)
SELECT NULL, 'Grace', 'Okafor', 'FEMALE', '1978-11-03', '+1 555 4103', 'grace.okafor@example.test',
       '12 River Road', 'B+', 'Latex sensitivity', 'Chidi Okafor +1 555 8103'
WHERE @doctor_primary_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM patients WHERE email = 'grace.okafor@example.test');

INSERT INTO patients(user_id, first_name, last_name, gender, date_of_birth, phone, email, address, blood_group, allergies, emergency_contact)
SELECT NULL, 'Ethan', 'Garcia', 'MALE', '2002-01-30', '+1 555 4104', 'ethan.garcia@example.test',
       '9 Oak Lane', 'AB+', 'Shellfish', 'Maria Garcia +1 555 8104'
WHERE @doctor_primary_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM patients WHERE email = 'ethan.garcia@example.test');

SET @patient_lina_id = (SELECT id FROM patients WHERE email = 'lina.patel@example.test');
SET @patient_sam_id = (SELECT id FROM patients WHERE email = 'sam.williams@example.test');
SET @patient_grace_id = (SELECT id FROM patients WHERE email = 'grace.okafor@example.test');
SET @patient_ethan_id = (SELECT id FROM patients WHERE email = 'ethan.garcia@example.test');

DELETE rq
FROM reminder_queue rq
JOIN appointments a ON a.id = rq.appointment_id
JOIN patients p ON p.id = a.patient_id
WHERE p.email LIKE '%@example.test';

DELETE pi
FROM prescription_items pi
JOIN prescriptions pr ON pr.id = pi.prescription_id
JOIN medical_history mh ON mh.id = pr.medical_history_id
JOIN patients p ON p.id = mh.patient_id
WHERE p.email LIKE '%@example.test';

DELETE pr
FROM prescriptions pr
JOIN medical_history mh ON mh.id = pr.medical_history_id
JOIN patients p ON p.id = mh.patient_id
WHERE p.email LIKE '%@example.test';

DELETE mh
FROM medical_history mh
JOIN patients p ON p.id = mh.patient_id
WHERE p.email LIKE '%@example.test';

DELETE a
FROM appointments a
JOIN patients p ON p.id = a.patient_id
WHERE p.email LIKE '%@example.test';

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_lina_id, @doctor_primary_id, CURDATE(), '09:30:00', 'SCHEDULED', 'Blood pressure follow-up', 'Bring home BP readings.'
WHERE @doctor_primary_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_sam_id, @doctor_secondary_id, CURDATE(), '10:30:00', 'PENDING', 'New patient consultation', 'Verify insurance details at check-in.'
WHERE @doctor_secondary_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_grace_id, @doctor_primary_id, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:00:00', 'SCHEDULED', 'Chest discomfort review', 'ECG requested if symptoms continue.'
WHERE @doctor_primary_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_ethan_id, @doctor_secondary_id, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:00:00', 'SCHEDULED', 'Allergy medication review', 'Patient reports seasonal flare-ups.'
WHERE @doctor_secondary_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_lina_id, @doctor_primary_id, DATE_SUB(CURDATE(), INTERVAL 7 DAY), '13:30:00', 'COMPLETED', 'Medication review', 'Visit completed and prescription issued.'
WHERE @doctor_primary_id IS NOT NULL;

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
SELECT @patient_sam_id, @doctor_secondary_id, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '15:00:00', 'CANCELLED', 'Routine follow-up', 'Patient rescheduled by phone.'
WHERE @doctor_secondary_id IS NOT NULL;

SET @completed_lina_appt_id = (
    SELECT id FROM appointments
    WHERE patient_id = @patient_lina_id
      AND doctor_id = @doctor_primary_id
      AND appointment_date = DATE_SUB(CURDATE(), INTERVAL 7 DAY)
      AND appointment_time = '13:30:00'
    ORDER BY id DESC LIMIT 1
);

INSERT INTO medical_history(patient_id, doctor_id, appointment_id, diagnosis, treatment_notes)
SELECT @patient_lina_id, @doctor_primary_id, @completed_lina_appt_id,
       'Essential hypertension, controlled',
       'Continue lifestyle plan and monitor blood pressure twice weekly.'
WHERE @completed_lina_appt_id IS NOT NULL;

SET @lina_history_id = LAST_INSERT_ID();

INSERT INTO prescriptions(medical_history_id, instructions)
SELECT @lina_history_id,
       'Take medication after breakfast. Return sooner if dizziness or persistent headache occurs.'
WHERE @lina_history_id > 0;

SET @lina_prescription_id = LAST_INSERT_ID();

INSERT INTO prescription_items(prescription_id, medicine_name, dosage, frequency, duration, notes)
SELECT @lina_prescription_id, 'Amlodipine', '5 mg', 'Once daily', '30 days', 'Review dose at next visit.'
WHERE @lina_prescription_id > 0;

INSERT INTO prescription_items(prescription_id, medicine_name, dosage, frequency, duration, notes)
SELECT @lina_prescription_id, 'Vitamin D3', '1000 IU', 'Once daily', '60 days', 'Take with food.'
WHERE @lina_prescription_id > 0;

INSERT INTO reminder_queue(appointment_id, recipient_email, recipient_phone, channel, status, message, scheduled_at)
SELECT a.id, p.email, p.phone, 'EMAIL', 'PENDING',
       CONCAT('Hello ', p.first_name, ', your appointment with Dr. ', u.full_name, ' is on ', a.appointment_date, ' at ', TIME_FORMAT(a.appointment_time, '%H:%i'), '.'),
       DATE_SUB(CONCAT(a.appointment_date, ' ', a.appointment_time), INTERVAL 1 DAY)
FROM appointments a
JOIN patients p ON p.id = a.patient_id
JOIN doctors d ON d.id = a.doctor_id
JOIN users u ON u.id = d.user_id
WHERE p.email LIKE '%@example.test'
  AND a.status IN ('SCHEDULED', 'PENDING')
  AND a.appointment_date >= CURDATE();

INSERT INTO audit_logs(actor_user_id, action, entity_type, entity_id, summary) VALUES
(NULL, 'SEED_DATA_LOADED', 'DATABASE', NULL, 'Fake clinical data loaded for local testing.'),
(NULL, 'CREATE', 'APPOINTMENT', NULL, 'Fake appointments created for workflow testing.'),
(NULL, 'CREATE', 'MEDICAL_HISTORY', @lina_history_id, 'Fake completed visit with prescription created.');

COMMIT;
