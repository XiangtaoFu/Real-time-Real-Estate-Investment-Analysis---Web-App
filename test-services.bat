@echo off
echo ========================================
echo Testing Backend Services
echo ========================================
echo.

echo [1] Testing Property Search API (port 8081)...
curl -X GET "http://localhost:8081/api/properties/search?postalCode=02215&limit=1" 2>nul
if %errorlevel% equ 0 (
    echo     ✓ Property Search API is running
) else (
    echo     ✗ Property Search API connection failed
)
echo.

echo [2] Testing Cashflow Calculator (port 8081)...
curl -X POST http://localhost:8081/api/analysis/cashflow -H "Content-Type: application/json" -d "{\"offerPrice\":849000,\"fmv\":1191700,\"grossRentsAnnual\":50000,\"numberOfUnits\":1}" 2>nul
if %errorlevel% equ 0 (
    echo     ✓ Cashflow Calculator integrated successfully
) else (
    echo     ✗ Cashflow Calculator not responding
)
echo.

echo [3] Testing Google Geo API (port 8080)...
curl -X GET "http://localhost:8080/api/geo/text?text=Boston&place=Massachusetts" 2>nul
if %errorlevel% equ 0 (
    echo     ✓ Google Geo API is running
) else (
    echo     ✗ Google Geo API connection failed
)
echo.

echo ========================================
echo Architecture Summary
echo ========================================
echo.
echo realtyinus (Port 8081):
echo   - Property Search API (Realty In US)
echo   - Cashflow Calculator (Investment Analysis)
echo   - Mortgage API Integration
echo.
echo googlemapv2 (Port 8080):
echo   - Google Maps Geo Location API
echo.
echo Data Flow:
echo   User → Property Search → Realty API → Data Mapping
echo                                       ↓
echo                          Cashflow Calculator → Results
echo.
echo ========================================
echo Test Complete
echo ========================================
echo.
pause
