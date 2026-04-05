@echo off
set MAVEN_HOME=%~dp0.tools\apache-maven-3.9.9
call "%MAVEN_HOME%\bin\mvn.cmd" %*
