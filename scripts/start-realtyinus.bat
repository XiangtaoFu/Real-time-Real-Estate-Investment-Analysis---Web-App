@echo off
echo ===============================================
echo Starting RealtyInUS Spring Boot Service
echo ===============================================
echo.

cd /d "%~dp0..\realtyinus"

echo Checking Maven installation...
call mvnw.cmd --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Maven wrapper not found, attempting to use system Maven...
    "C:\Program Files\apache-maven-3.9.11\bin\mvn.cmd" --version >nul 2>&1
    if %errorlevel% neq 0 (
        echo ERROR: Maven not found. Please install Maven or use Maven wrapper.
        pause
        exit /b 1
    )
    set MAVEN_CMD="C:\Program Files\apache-maven-3.9.11\bin\mvn.cmd"
) else (
    set MAVEN_CMD=mvnw.cmd
)

echo.
echo Cleaning and building project...
call %MAVEN_CMD% clean install -DskipTests

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Build failed. Please check the error messages above.
    pause
    exit /b 1
)

echo.
echo ===============================================
echo Starting Spring Boot Application on port 8080
echo ===============================================
echo.
echo Available endpoints:
echo   - GET  http://localhost:8080/api/properties/search
echo   - GET  http://localhost:8080/api/properties/{propertyId}
echo   - GET  http://localhost:8080/api/properties/{propertyId}/investment-data
echo.
echo Press Ctrl+C to stop the server
echo ===============================================
echo.

call %MAVEN_CMD% spring-boot:run
