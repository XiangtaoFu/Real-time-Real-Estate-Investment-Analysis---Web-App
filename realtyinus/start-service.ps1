# Start RealtyInUS Service
Write-Host "=== Starting RealtyInUS Service ===" -ForegroundColor Cyan

# Set environment
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot"
$MAVEN_CMD = "C:\Program Files\apache-maven-3.9.11\bin\mvn.cmd"

# Change to project directory
Set-Location "c:\Users\14215\Desktop\Real-Time-Real-Estate-Investment-Analysis--Web-App\Real-Time-Real-Estate-Investment-Analysis--Web-App\realtyinus"

Write-Host "Starting Maven Spring Boot application..." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop the service" -ForegroundColor Yellow
Write-Host ""

# Run the service
& $MAVEN_CMD spring-boot:run
