@echo off
echo Testing single property search endpoint...
curl -X POST "http://localhost:8081/api/properties/search?postalCode=02115&limit=1" ^
  -H "Content-Type: application/json" ^
  -w "\n\nHTTP Status: %%{http_code}\n"
pause
