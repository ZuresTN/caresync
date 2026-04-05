# CareSync MVP Stabilization Plan

## Implemented Focus
- Appointment conflict prevention for active doctor slots.
- Admin-controlled temporary password reset with forced password change.
- Audit logging for core security and record actions.
- Reminder queue with template rendering and SMTP email delivery settings.
- Stronger validation for passwords, contacts, appointments, records, prescriptions, and settings.
- JUnit test coverage for the highest-risk service logic.

## Manual Database Verification
- For a new clinic database, import `database/caresync_mysql.sql`.
- For an existing database, run `database/caresync_migration.sql`.
- Confirm these objects exist: `audit_logs`, `reminder_queue`, `users.password_must_change`, `users.updated_at`, and `appointments.idx_appointment_conflict`.

## Manual App Checks
- Sign in with a real administrator account. On an empty database, set `CARESYNC_INITIAL_ADMIN_PASSWORD` before first startup.
- Create or edit a user, then use `Reset Password`; sign in as that user and confirm the forced change-password screen appears.
- Create an active appointment, then try creating another active appointment for the same doctor/date/time; it should be blocked.
- Cancel an appointment, then rebook the same slot; it should be allowed.
- Queue a reminder from Appointment Calendar; if SMTP is disabled, `Send Due` should fail gracefully and mark queued reminders as failed.
- Open Admin `Audit Logs` and confirm user, appointment, reminder, settings, medical record, and PDF actions appear.
- Generate a prescription PDF from medical history or patient dashboard.

## Test Commands
```bat
mvnw.cmd clean compile
mvnw.cmd test
```
