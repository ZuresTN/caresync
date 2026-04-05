@echo off
cd /d "%~dp0"
echo Starting CareSync...
echo Make sure the clinic database service is running.
call "%~dp0mvnw.cmd" javafx:run
