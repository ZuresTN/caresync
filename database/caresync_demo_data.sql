USE caresync;

START TRANSACTION;

-- CareSync full demo dataset.
-- Password for all demo accounts: CareSync123!
-- Usernames:
--   demo_admin
--   demo_doctor_maya
--   demo_doctor_omar
--   demo_reception_ana
--   demo_reception_jules
-- Patients are clinic records only. Their email fields are used for reminders.

SET @demo_password_hash = '$2a$12$DfMX9AbAOMdq2iCWKpxXr.9EdITz0CIs.yO2iWVSxYEkapH75ykRu';

-- Reset this demo dataset so re-running the file stays clean.
DELETE FROM patients
WHERE email LIKE '%@demo.caresync.test';

DELETE FROM users
WHERE username IN (
    'demo_admin',
    'demo_doctor_maya',
    'demo_doctor_omar',
    'demo_reception_ana',
    'demo_reception_jules',
    'demo_patient_lina',
    'demo_patient_sam'
);

INSERT INTO users(full_name, username, password_hash, role, active, password_must_change) VALUES
('Demo Administrator', 'demo_admin', @demo_password_hash, 'ADMIN', TRUE, FALSE),
('Dr. Maya Chen', 'demo_doctor_maya', @demo_password_hash, 'DOCTOR', TRUE, FALSE),
('Dr. Omar Mensah', 'demo_doctor_omar', @demo_password_hash, 'DOCTOR', TRUE, FALSE),
('Ana Rivera', 'demo_reception_ana', @demo_password_hash, 'RECEPTIONIST', TRUE, FALSE),
('Jules Carter', 'demo_reception_jules', @demo_password_hash, 'RECEPTIONIST', TRUE, FALSE);

SET @admin_user_id = (SELECT id FROM users WHERE username = 'demo_admin');
SET @doctor_maya_user_id = (SELECT id FROM users WHERE username = 'demo_doctor_maya');
SET @doctor_omar_user_id = (SELECT id FROM users WHERE username = 'demo_doctor_omar');
SET @reception_ana_user_id = (SELECT id FROM users WHERE username = 'demo_reception_ana');
SET @reception_jules_user_id = (SELECT id FROM users WHERE username = 'demo_reception_jules');

INSERT INTO doctors(user_id, specialization, phone, email, room, availability) VALUES
(@doctor_maya_user_id, 'Cardiology', '+1 555 2201', 'maya.chen@demo.caresync.test', 'Room 201', 'Mon-Wed-Fri, 09:00-15:00'),
(@doctor_omar_user_id, 'Family Medicine', '+1 555 2202', 'omar.mensah@demo.caresync.test', 'Room 114', 'Tue-Thu, 10:00-17:00; Sat, 09:00-12:00');

INSERT INTO receptionists(user_id, phone, email, shift) VALUES
(@reception_ana_user_id, '+1 555 2301', 'ana.rivera@demo.caresync.test', 'Morning shift'),
(@reception_jules_user_id, '+1 555 2302', 'jules.carter@demo.caresync.test', 'Afternoon shift');

SET @doctor_maya_id = (SELECT id FROM doctors WHERE user_id = @doctor_maya_user_id);
SET @doctor_omar_id = (SELECT id FROM doctors WHERE user_id = @doctor_omar_user_id);

INSERT INTO patients(user_id, first_name, last_name, gender, date_of_birth, phone, email, address, blood_group, allergies, emergency_contact) VALUES
(NULL, 'Lina', 'Patel', 'FEMALE', '1991-04-12', '+1 555 4101', 'lina.patel@demo.caresync.test', '45 Maple Street', 'O+', 'Penicillin', 'Arjun Patel +1 555 8101'),
(NULL, 'Sam', 'Williams', 'MALE', '1984-09-22', '+1 555 4102', 'sam.williams@demo.caresync.test', '78 Cedar Avenue', 'A-', 'None reported', 'Nora Williams +1 555 8102'),
(NULL, 'Grace', 'Okafor', 'FEMALE', '1978-11-03', '+1 555 4103', 'grace.okafor@demo.caresync.test', '12 River Road', 'B+', 'Latex sensitivity', 'Chidi Okafor +1 555 8103'),
(NULL, 'Ethan', 'Garcia', 'MALE', '2002-01-30', '+1 555 4104', 'ethan.garcia@demo.caresync.test', '9 Oak Lane', 'AB+', 'Shellfish', 'Maria Garcia +1 555 8104'),
(NULL, 'Nora', 'Bennett', 'FEMALE', '1969-06-18', '+1 555 4105', 'nora.bennett@demo.caresync.test', '300 Pine Court', 'O-', 'Sulfa drugs', 'Milo Bennett +1 555 8105'),
(NULL, 'Aiden', 'Brooks', 'MALE', '2015-02-07', '+1 555 4106', 'aiden.brooks@demo.caresync.test', '19 Willow Drive', 'A+', 'Peanuts', 'Tara Brooks +1 555 8106'),
(NULL, 'Mina', 'Haddad', 'FEMALE', '1988-12-14', '+1 555 4107', 'mina.haddad@demo.caresync.test', '66 Lake View', 'B-', 'Ibuprofen', 'Karim Haddad +1 555 8107'),
(NULL, 'Leo', 'Nguyen', 'MALE', '1959-08-05', '+1 555 4108', 'leo.nguyen@demo.caresync.test', '144 Garden Road', 'AB-', 'None reported', 'Mai Nguyen +1 555 8108');

SET @patient_lina_id = (SELECT id FROM patients WHERE email = 'lina.patel@demo.caresync.test');
SET @patient_sam_id = (SELECT id FROM patients WHERE email = 'sam.williams@demo.caresync.test');
SET @patient_grace_id = (SELECT id FROM patients WHERE email = 'grace.okafor@demo.caresync.test');
SET @patient_ethan_id = (SELECT id FROM patients WHERE email = 'ethan.garcia@demo.caresync.test');
SET @patient_nora_id = (SELECT id FROM patients WHERE email = 'nora.bennett@demo.caresync.test');
SET @patient_aiden_id = (SELECT id FROM patients WHERE email = 'aiden.brooks@demo.caresync.test');
SET @patient_mina_id = (SELECT id FROM patients WHERE email = 'mina.haddad@demo.caresync.test');
SET @patient_leo_id = (SELECT id FROM patients WHERE email = 'leo.nguyen@demo.caresync.test');

INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes) VALUES
(@patient_lina_id, @doctor_maya_id, CURDATE(), '09:00:00', 'SCHEDULED', 'Blood pressure follow-up', 'Bring home BP readings.'),
(@patient_sam_id, @doctor_omar_id, CURDATE(), '09:45:00', 'PENDING', 'New patient consultation', 'Verify insurance details at check-in.'),
(@patient_grace_id, @doctor_maya_id, CURDATE(), '10:30:00', 'SCHEDULED', 'Chest discomfort review', 'ECG requested if symptoms continue.'),
(@patient_ethan_id, @doctor_omar_id, CURDATE(), '11:15:00', 'SCHEDULED', 'Allergy medication review', 'Patient reports seasonal flare-ups.'),
(@patient_nora_id, @doctor_maya_id, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:00:00', 'SCHEDULED', 'Diabetes care review', 'Review glucose diary and foot exam.'),
(@patient_aiden_id, @doctor_omar_id, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:30:00', 'PENDING', 'Pediatric fever follow-up', 'Parent requested same-week review.'),
(@patient_mina_id, @doctor_maya_id, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '10:00:00', 'SCHEDULED', 'Migraine medication review', 'Discuss triggers and medication tolerance.'),
(@patient_leo_id, @doctor_omar_id, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '15:00:00', 'SCHEDULED', 'Annual wellness visit', 'Fasting labs completed.'),
(@patient_lina_id, @doctor_maya_id, DATE_SUB(CURDATE(), INTERVAL 7 DAY), '13:30:00', 'COMPLETED', 'Medication review', 'Visit completed and prescription issued.'),
(@patient_sam_id, @doctor_omar_id, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '15:00:00', 'COMPLETED', 'Respiratory infection review', 'Symptoms improving; medication given.'),
(@patient_grace_id, @doctor_maya_id, DATE_SUB(CURDATE(), INTERVAL 14 DAY), '11:30:00', 'COMPLETED', 'ECG result consultation', 'No acute findings. Lifestyle guidance discussed.'),
(@patient_ethan_id, @doctor_omar_id, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '16:00:00', 'CANCELLED', 'Routine follow-up', 'Patient rescheduled by phone.'),
(@patient_nora_id, @doctor_maya_id, DATE_SUB(CURDATE(), INTERVAL 21 DAY), '10:15:00', 'COMPLETED', 'Diabetes medication adjustment', 'Adjusted metformin timing.'),
(@patient_leo_id, @doctor_omar_id, DATE_SUB(CURDATE(), INTERVAL 30 DAY), '09:30:00', 'COMPLETED', 'Hypertension check', 'Stable readings in clinic.');

SET @appt_lina_completed = (
    SELECT id FROM appointments
    WHERE patient_id = @patient_lina_id AND appointment_date = DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND appointment_time = '13:30:00'
    LIMIT 1
);
SET @appt_sam_completed = (
    SELECT id FROM appointments
    WHERE patient_id = @patient_sam_id AND appointment_date = DATE_SUB(CURDATE(), INTERVAL 5 DAY) AND appointment_time = '15:00:00'
    LIMIT 1
);
SET @appt_grace_completed = (
    SELECT id FROM appointments
    WHERE patient_id = @patient_grace_id AND appointment_date = DATE_SUB(CURDATE(), INTERVAL 14 DAY) AND appointment_time = '11:30:00'
    LIMIT 1
);
SET @appt_nora_completed = (
    SELECT id FROM appointments
    WHERE patient_id = @patient_nora_id AND appointment_date = DATE_SUB(CURDATE(), INTERVAL 21 DAY) AND appointment_time = '10:15:00'
    LIMIT 1
);
SET @appt_leo_completed = (
    SELECT id FROM appointments
    WHERE patient_id = @patient_leo_id AND appointment_date = DATE_SUB(CURDATE(), INTERVAL 30 DAY) AND appointment_time = '09:30:00'
    LIMIT 1
);

INSERT INTO medical_history(patient_id, doctor_id, appointment_id, diagnosis, treatment_notes) VALUES
(@patient_lina_id, @doctor_maya_id, @appt_lina_completed, 'Essential hypertension, controlled', 'Continue lifestyle plan and monitor blood pressure twice weekly.'),
(@patient_sam_id, @doctor_omar_id, @appt_sam_completed, 'Acute upper respiratory infection', 'Hydration, rest, and symptom monitoring. Return if fever persists.'),
(@patient_grace_id, @doctor_maya_id, @appt_grace_completed, 'Non-cardiac chest discomfort', 'ECG reviewed. Discussed reflux precautions and stress management.'),
(@patient_nora_id, @doctor_maya_id, @appt_nora_completed, 'Type 2 diabetes mellitus', 'Medication timing adjusted. Reinforced glucose tracking and nutrition plan.'),
(@patient_leo_id, @doctor_omar_id, @appt_leo_completed, 'Primary hypertension', 'Clinic BP stable. Continue medication and low-sodium diet.');

SET @history_lina_id = (SELECT id FROM medical_history WHERE appointment_id = @appt_lina_completed LIMIT 1);
SET @history_sam_id = (SELECT id FROM medical_history WHERE appointment_id = @appt_sam_completed LIMIT 1);
SET @history_grace_id = (SELECT id FROM medical_history WHERE appointment_id = @appt_grace_completed LIMIT 1);
SET @history_nora_id = (SELECT id FROM medical_history WHERE appointment_id = @appt_nora_completed LIMIT 1);
SET @history_leo_id = (SELECT id FROM medical_history WHERE appointment_id = @appt_leo_completed LIMIT 1);

INSERT INTO prescriptions(medical_history_id, instructions) VALUES
(@history_lina_id, 'Take medication after breakfast. Return sooner if dizziness or persistent headache occurs.'),
(@history_sam_id, 'Take after meals. Avoid driving if drowsy.'),
(@history_grace_id, 'Use only as needed. Follow up if discomfort returns.'),
(@history_nora_id, 'Take with meals and record fasting glucose each morning.'),
(@history_leo_id, 'Take at the same time daily. Bring BP log to next visit.');

SET @rx_lina_id = (SELECT id FROM prescriptions WHERE medical_history_id = @history_lina_id LIMIT 1);
SET @rx_sam_id = (SELECT id FROM prescriptions WHERE medical_history_id = @history_sam_id LIMIT 1);
SET @rx_grace_id = (SELECT id FROM prescriptions WHERE medical_history_id = @history_grace_id LIMIT 1);
SET @rx_nora_id = (SELECT id FROM prescriptions WHERE medical_history_id = @history_nora_id LIMIT 1);
SET @rx_leo_id = (SELECT id FROM prescriptions WHERE medical_history_id = @history_leo_id LIMIT 1);

INSERT INTO prescription_items(prescription_id, medicine_name, dosage, frequency, duration, notes) VALUES
(@rx_lina_id, 'Amlodipine', '5 mg', 'Once daily', '30 days', 'Review dose at next visit.'),
(@rx_lina_id, 'Vitamin D3', '1000 IU', 'Once daily', '60 days', 'Take with food.'),
(@rx_sam_id, 'Cetirizine', '10 mg', 'Once daily', '7 days', 'Take in the evening if drowsy.'),
(@rx_sam_id, 'Saline nasal spray', '2 sprays', 'Twice daily', '5 days', 'Use as needed for congestion.'),
(@rx_grace_id, 'Omeprazole', '20 mg', 'Once daily', '14 days', 'Take before breakfast.'),
(@rx_nora_id, 'Metformin', '500 mg', 'Twice daily', '30 days', 'Take with meals.'),
(@rx_nora_id, 'Glucose test strips', '1 strip', 'Every morning', '30 days', 'Record fasting readings.'),
(@rx_leo_id, 'Losartan', '50 mg', 'Once daily', '30 days', 'Monitor for dizziness.');

INSERT INTO reminder_queue(appointment_id, recipient_email, recipient_phone, channel, status, message, scheduled_at)
SELECT a.id, p.email, p.phone, 'EMAIL', 'PENDING',
       CONCAT('Hello ', p.first_name, ', your appointment with Dr. ', u.full_name, ' is on ', a.appointment_date, ' at ', TIME_FORMAT(a.appointment_time, '%H:%i'), '.'),
       DATE_SUB(CONCAT(a.appointment_date, ' ', a.appointment_time), INTERVAL 1 DAY)
FROM appointments a
JOIN patients p ON p.id = a.patient_id
JOIN doctors d ON d.id = a.doctor_id
JOIN users u ON u.id = d.user_id
WHERE p.email LIKE '%@demo.caresync.test'
  AND a.status IN ('SCHEDULED', 'PENDING')
  AND a.appointment_date >= CURDATE();

INSERT INTO reminder_queue(appointment_id, recipient_email, recipient_phone, channel, status, message, scheduled_at, sent_at)
SELECT a.id, p.email, p.phone, 'EMAIL', 'SENT',
       CONCAT('Hello ', p.first_name, ', thank you for visiting CareSync.'),
       DATE_SUB(CONCAT(a.appointment_date, ' ', a.appointment_time), INTERVAL 1 DAY),
       DATE_SUB(CONCAT(a.appointment_date, ' ', a.appointment_time), INTERVAL 1 DAY)
FROM appointments a
JOIN patients p ON p.id = a.patient_id
WHERE p.email LIKE '%@demo.caresync.test'
  AND a.status = 'COMPLETED'
LIMIT 3;

INSERT INTO audit_logs(actor_user_id, action, entity_type, entity_id, summary) VALUES
(@admin_user_id, 'DEMO_DATA_LOADED', 'DATABASE', NULL, 'Demo dataset loaded for CareSync visual testing.'),
(@reception_ana_user_id, 'CREATE', 'APPOINTMENT', NULL, 'Demo appointments created for calendar and dashboard testing.'),
(@doctor_maya_user_id, 'CREATE', 'MEDICAL_HISTORY', @history_lina_id, 'Demo medical history and prescription created.'),
(@doctor_omar_user_id, 'CREATE', 'MEDICAL_HISTORY', @history_sam_id, 'Demo medical history and prescription created.');

COMMIT;
