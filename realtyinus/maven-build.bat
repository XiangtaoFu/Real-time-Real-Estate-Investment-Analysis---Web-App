@echo off
REM Simple Maven Build Script
REM This script will download Maven if needed and run it

setlocal

REM Set JAVA_HOME if not set (prefer LTS JDKs compatible with Spring Boot 3.x)
if "%JAVA_HOME%" == "" (
    echo JAVA_HOME not set. Attempting to locate a compatible JDK (21 or 17)...
    for /f "delims=" %%D in ('dir /b /ad "C:\Program Files\Eclipse Adoptium\jdk-21*" 2^>nul') do set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    if "%JAVA_HOME%" == "" (
        for /f "delims=" %%D in ('dir /b /ad "C:\Program Files\Eclipse Adoptium\jdk-17*" 2^>nul') do set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\%%D"
    )
    if "%JAVA_HOME%" == "" (
        for /f "delims=" %%D in ('dir /b /ad "C:\Program Files\Java\jdk-21*" 2^>nul') do set "JAVA_HOME=C:\Program Files\Java\%%D"
    )
    if "%JAVA_HOME%" == "" (
        for /f "delims=" %%D in ('dir /b /ad "C:\Program Files\Java\jdk-17*" 2^>nul') do set "JAVA_HOME=C:\Program Files\Java\%%D"
    )
)

echo ========================================
echo Maven Build Script for RealtyInUS
echo ========================================
echo.
if not "%JAVA_HOME%" == "" (
    echo Java Home: %JAVA_HOME%
) else (
    echo Java Home: [not set - relying on system java in PATH]
)
echo.

REM Check if Maven is installed
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Maven found in PATH
    mvn %*
    goto :end
)

REM Check for portable Maven
set "MAVEN_HOME=%~dp0.maven\apache-maven-3.9.5"
if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Using portable Maven from %MAVEN_HOME%
    "%MAVEN_HOME%\bin\mvn.cmd" %*
    goto :end
)

REM Download portable Maven
echo Maven not found. Downloading portable Maven...
echo.

set "DOWNLOAD_DIR=%~dp0.maven"
if not exist "%DOWNLOAD_DIR%" mkdir "%DOWNLOAD_DIR%"

echo Downloading Maven 3.9.5...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.zip' " ^
    "-OutFile '%DOWNLOAD_DIR%\maven.zip'"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to download Maven
    goto :error
)

echo Extracting Maven...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Expand-Archive -Path '%DOWNLOAD_DIR%\maven.zip' -DestinationPath '%DOWNLOAD_DIR%' -Force"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to extract Maven
    goto :error
)

del "%DOWNLOAD_DIR%\maven.zip"

echo Maven downloaded successfully!
echo.

REM Run Maven
"%MAVEN_HOME%\bin\mvn.cmd" %*
goto :end

:error
echo.
echo ========================================
echo ERROR: Maven setup failed
echo ========================================
echo.
echo Please install Maven manually:
echo 1. Download from: https://maven.apache.org/download.cgi
echo 2. Extract to C:\Program Files\Maven
echo 3. Add C:\Program Files\Maven\bin to PATH
echo.
pause
exit /b 1

:end
endlocal

