# CareSync

CareSync is a JavaFX desktop clinic management system built with Maven, JavaFX FXML, Scene Builder-friendly layouts, MySQL, BCrypt password hashing, and OpenPDF prescription generation.

## Project Structure

```text
caresync/
├── pom.xml
├── database/
│   └── caresync_mysql.sql
└── src/
    └── main/
        ├── java/
        │   └── com/caresync/
        │       ├── Main.java
        │       ├── config/
        │       ├── controllers/
        │       ├── dao/
        │       ├── models/
        │       ├── services/
        │       └── utils/
        └── resources/
            ├── database.properties
            ├── css/app.css
            ├── fxml/
            └── images/
```

## Features Included

- Login, logout, role-based navigation, and BCrypt password hashing.
- Roles: Admin, Doctor, Receptionist, Patient.
- Admin dashboard with user, doctor, receptionist, patient, and appointment statistics.
- Admin user management with linked doctor, receptionist, and patient profile fields.
- Dedicated Admin doctor management screen for adding and updating doctors.
- Dedicated Admin receptionist management screen for adding and updating receptionists.
- Production-first account setup with no sample staff accounts or hardcoded staff passwords.
- Patient profile CRUD with contact information, allergies, emergency contact, and search.
- Appointment CRUD with status filters, date filters, doctor availability checks, conflict prevention, weekly calendar tiles, cancel/complete actions, and queued email reminders.
- Doctor dashboard for assigned appointments.
- Patient dashboard for appointment reminders, medical history, and receiving prescription PDFs.
- Medical history view, diagnosis entry, treatment notes, prescription entry, and PDF prescription generation.
- Professional prescription PDFs with patient details, diagnosis, medication table, instructions, signature areas, and automatic open after generation.
- System settings screen with clinic identity, reminder template, and SMTP email settings.
- Admin password reset with a generated temporary password and forced password change on next login.
- Audit logs for security and record activity.
- Modern JavaFX FXML UI using shared CSS, sidebar navigation, hover effects, rounded inputs, animated page transitions, loading screen, and dashboard cards.

## Requirements

- JDK 17 or newer.
- Maven. A project-local launcher is included as `mvnw.cmd`.
- MySQL or MariaDB database service.
- Scene Builder, optional, for editing files in `src/main/resources/fxml/`.

## Database Setup

Create a `caresync` database and import the schema:

```sql
mysql -u <clinic_db_admin> -p < database/caresync_mysql.sql
```

For an existing CareSync database, apply the migration:

```sql
mysql -u <clinic_db_admin> -p < database/caresync_migration.sql
```

To load optional fake clinic data for testing screens and workflows:

```bash
load-test-data.cmd
```

The seed data does not create login accounts. It uses the existing doctors in your clinic database, then adds fake patient profiles, current/future appointments, cancelled/completed appointments, reminder queue rows, audit log rows, and one completed medical record with prescription items. Create at least one real doctor in the app before loading this file.

The app reads database settings from environment variables first, then Java system properties, then `src/main/resources/database.properties`:

```properties
CARESYNC_DB_URL=jdbc:mysql://localhost:3306/caresync?useSSL=false&serverTimezone=UTC
CARESYNC_DB_USERNAME=caresync_app
CARESYNC_DB_PASSWORD=<secure password>
```

Fallback file keys:

```properties
db.url=jdbc:mysql://localhost:3306/caresync?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=caresync_app
db.password=<secure password>
```

For first startup on an empty database, set:

```properties
CARESYNC_INITIAL_ADMIN_PASSWORD=<temporary secure password>
CARESYNC_INITIAL_ADMIN_USERNAME=admin
CARESYNC_INITIAL_ADMIN_NAME=Clinic Administrator
```

The initial admin is forced to change the password after first login.

## Run The App

From the project root:

```bash
mvnw.cmd clean javafx:run
```

To compile without running:

```bash
mvnw.cmd clean compile
```

## Gmail Appointment Reminders

Open `Settings`, click `Use Gmail`, enter your Gmail address, and enter a Google app password in the password field. Do not use your normal Gmail password. Click `Use CareSync Template` to load the default patient reminder template, then save settings.

Reminders are queued from Appointment Management. The app sends due queued reminders in the background while it is running, and the `Send Due` button can be used to trigger due reminder sending manually.

Patients do not need login accounts for reminders. Add the patient in `Patient Management` with a reminder email, then create the appointment and queue the reminder.

When a doctor saves a medical record with a prescription item, CareSync generates the medical report PDF and sends it to the patient's reminder email if SMTP is enabled. In `Settings`, use `Send Test Prescription PDF` to verify Gmail SMTP attachments before using it with real patients.

## Scene Builder

Open any file in `src/main/resources/fxml/` with Scene Builder. Controllers live in `src/main/java/com/caresync/controllers/`, and shared styling lives in `src/main/resources/css/app.css`.

## Generated PDFs

Medical report PDFs are written to:

```text
generated-prescriptions/
```

The generated report includes the CareSync logo, clinic details, patient demographics, clinical notes, prescription items, instructions, and a clinician signature area.

## Notes For Beginners

- Models in `models/` represent database records.
- DAO classes in `dao/` contain SQL queries.
- Services in `services/` hold shared application behavior like login, hashing, reminders, and PDF generation.
- Controllers in `controllers/` connect FXML screens to DAO and service logic.
- FXML files in `resources/fxml/` define the UI layout.
- `app.css` controls the premium medical dashboard styling.
