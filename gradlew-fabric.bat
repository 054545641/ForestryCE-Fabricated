@echo off
setlocal
set "JAVA_HOME=%~dp0.local\jdk25"
set "GRADLE_USER_HOME=%~dp0.local\gradle-fabric-26.2"
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo Local JDK 25 is missing. Run powershell -ExecutionPolicy Bypass -File "%~dp0scripts\setup-fabric.ps1"
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
pushd "%~dp0"
call "%~dp0gradlew.bat" --project-cache-dir "%~dp0.local\project-cache-fabric-26.2-root" %*
set "GRADLE_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %GRADLE_EXIT_CODE%
