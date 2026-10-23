@echo off
rem The active branch uses Fabric. NeoForge development remains on 26.2-neoforge.
call "%~dp0gradlew-fabric.bat" %*
exit /b %ERRORLEVEL%
