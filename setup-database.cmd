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

echo Importing CareSync database...
if "%CARESYNC_DB_ADMIN_PASSWORD%"=="" (
  "%MYSQL_EXE%" -u "%CARESYNC_DB_ADMIN_USER%" < "%~dp0database\caresync_mysql.sql"
) else (
  "%MYSQL_EXE%" -u "%CARESYNC_DB_ADMIN_USER%" -p"%CARESYNC_DB_ADMIN_PASSWORD%" < "%~dp0database\caresync_mysql.sql"
)
if errorlevel 1 (
  echo.
  echo Initial import failed. Make sure the database service is running and CARESYNC_DB_ADMIN_USER / CARESYNC_DB_ADMIN_PASSWORD are set.
  exit /b 1
)

if "%CARESYNC_DB_ADMIN_PASSWORD%"=="" (
  "%MYSQL_EXE%" -u "%CARESYNC_DB_ADMIN_USER%" < "%~dp0database\caresync_migration.sql"
) else (
  "%MYSQL_EXE%" -u "%CARESYNC_DB_ADMIN_USER%" -p"%CARESYNC_DB_ADMIN_PASSWORD%" < "%~dp0database\caresync_migration.sql"
)

if errorlevel 1 (
  echo.
  echo Migration failed. Check database permissions and connection settings.
  exit /b 1
)

echo.
echo CareSync database is ready.
