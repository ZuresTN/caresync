@echo off
if "%MYSQL_EXE%"=="" set "MYSQL_EXE=mysql.exe"

if not exist "%MYSQL_EXE%" (
  if exist "C:\xampp1\mysql\bin\mysql.exe" set "MYSQL_EXE=C:\xampp1\mysql\bin\mysql.exe"
)

if not exist "%MYSQL_EXE%" (
  where mysql.exe >nul 2>nul
  if errorlevel 1 (
    echo MySQL client was not found. Add mysql.exe to PATH or update MYSQL_EXE in this file.
    exit /b 1
  )
  for /f "delims=" %%i in ('where mysql.exe') do if exist "%%i" set "MYSQL_EXE=%%i"
)

if "%CARESYNC_DB_ADMIN_USER%"=="" set "CARESYNC_DB_ADMIN_USER=root"

echo Loading CareSync test data...
if "%CARESYNC_DB_ADMIN_PASSWORD%"=="" (
  "%MYSQL_EXE%" -u "%CARESYNC_DB_ADMIN_USER%" < "%~dp0database\caresync_demo_data.sql"
) else (
  "%MYSQL_EXE%" -u "%CARESYNC_DB_ADMIN_USER%" -p"%CARESYNC_DB_ADMIN_PASSWORD%" < "%~dp0database\caresync_demo_data.sql"
)
if errorlevel 1 (
  echo.
  echo Test data import failed. Make sure the database service is running and CARESYNC_DB_ADMIN_USER / CARESYNC_DB_ADMIN_PASSWORD are set.
  exit /b 1
)

echo.
echo CareSync test data is ready.
echo It added fake role accounts, doctors, receptionists, patients, appointments, medical history, prescriptions, reminders, and audit rows.
echo Demo password for all demo accounts: CareSync123!
echo Demo users: demo_admin, demo_doctor_maya, demo_doctor_omar, demo_reception_ana, demo_reception_jules, demo_patient_lina, demo_patient_sam.
