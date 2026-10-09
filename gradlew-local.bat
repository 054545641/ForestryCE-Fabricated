@echo off
setlocal
set "JAVA_HOME=%~dp0.local\jdk21"
set "GRADLE_USER_HOME=%~dp0.local\gradle"
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo Local JDK 21 is missing. Run powershell -ExecutionPolicy Bypass -File "%~dp0scripts\setup-local.ps1"
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
pushd "%~dp0"
call "%~dp0gradlew.bat" %*
set "GRADLE_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %GRADLE_EXIT_CODE%
