@echo off
echo Starting googlemapv2 service on port 8080...
cd /d "%~dp0..\googlemapv2"
start "Cashflow Calculator API - Port 8080" cmd /k "C:\Program Files\apache-maven-3.9.11\bin\mvn.cmd" spring-boot:run

timeout /t 5 /nobreak >nul

echo Starting realtyinus service on port 8081...
cd /d "%~dp0..\realtyinus"
start "Property Search API - Port 8081" cmd /k "C:\Program Files\apache-maven-3.9.11\bin\mvn.cmd" spring-boot:run

echo.
echo ========================================
echo Both services are starting...
echo ========================================
echo.
echo [1] Cashflow Calculator API: http://localhost:8080
echo [2] Property Search API:     http://localhost:8081
echo.
echo Wait 30-60 seconds for services to fully start.
echo.
pause
