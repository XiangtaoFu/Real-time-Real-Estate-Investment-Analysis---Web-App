@echo off
echo ===================================================
echo Testing API Connection for Real Estate Web App
echo ===================================================
echo.

echo [1] Testing Cashflow Calculator API...
curl -X POST http://localhost:8080/api/analysis/cashflow ^
  -H "Content-Type: application/json" ^
  -d "{\"offerPrice\":850000,\"fmv\":875500,\"grossRentsAnnual\":62400}" ^
  --connect-timeout 5 ^
  --max-time 10 2>nul
echo.
echo.

echo [2] Testing Property Search API...
curl "http://localhost:8080/api/properties/search?location=Boston&status=for_sale&page=1" ^
  --connect-timeout 5 ^
  --max-time 10 2>nul
echo.
echo.

echo [3] Testing Google Maps Geo API...
curl "http://localhost:8080/api/geo/text?text=Boston&place=Massachusetts" ^
  --connect-timeout 5 ^
  --max-time 10 2>nul
echo.
echo.

echo ===================================================
echo API Connection Test Complete
echo ===================================================
pause
