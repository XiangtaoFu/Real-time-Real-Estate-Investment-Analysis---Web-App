@echo off
echo ===============================================
echo Testing RealtyInUS API Endpoints
echo ===============================================
echo.

echo Waiting for server to be ready...
timeout /t 3 /nobreak >nul

echo.
echo [TEST 1] Search properties in Boston (02215)
echo ===============================================
curl -s "http://localhost:8080/api/properties/search?postalCode=02215&limit=5" > test-search-result.json
if %errorlevel% equ 0 (
    echo SUCCESS: Search endpoint working
    type test-search-result.json | findstr /C:"properties" >nul
    if %errorlevel% equ 0 (
        echo Found properties in response
    )
) else (
    echo FAILED: Search endpoint not responding
)

echo.
echo.
echo [TEST 2] Get property details
echo ===============================================
echo Extracting first property ID from search results...

for /f "tokens=2 delims=:" %%a in ('type test-search-result.json ^| findstr /C:"propertyId"') do (
    set PROP_ID=%%a
    goto :found_id
)
:found_id

if defined PROP_ID (
    set PROP_ID=%PROP_ID:"=%
    set PROP_ID=%PROP_ID:,=%
    set PROP_ID=%PROP_ID: =%
    
    echo Testing with property ID: %PROP_ID%
    curl -s "http://localhost:8080/api/properties/%PROP_ID%" > test-detail-result.json
    
    if %errorlevel% equ 0 (
        echo SUCCESS: Detail endpoint working
        type test-detail-result.json | findstr /C:"cashflowDefaults" >nul
        if %errorlevel% equ 0 (
            echo Found cashflow defaults in response
        )
    ) else (
        echo FAILED: Detail endpoint not responding
    )
) else (
    echo SKIPPED: No property ID found in search results
)

echo.
echo.
echo [TEST 3] Get investment data
echo ===============================================
if defined PROP_ID (
    curl -s "http://localhost:8080/api/properties/%PROP_ID%/investment-data" > test-investment-result.json
    
    if %errorlevel% equ 0 (
        echo SUCCESS: Investment data endpoint working
        type test-investment-result.json | findstr /C:"dataCompleteness" >nul
        if %errorlevel% equ 0 (
            echo Found data completeness assessment
        )
    ) else (
        echo FAILED: Investment data endpoint not responding
    )
) else (
    echo SKIPPED: No property ID available
)

echo.
echo ===============================================
echo Test Results Summary
echo ===============================================
echo Test files generated:
echo   - test-search-result.json
echo   - test-detail-result.json
echo   - test-investment-result.json
echo.
echo Review these files to see the full API responses.
echo ===============================================
pause
