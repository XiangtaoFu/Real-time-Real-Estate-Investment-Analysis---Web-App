# End-to-End Demo Guide (English)

This short guide shows how to run the backend and demonstrate the full workflow:
Search by ZIP → pick a property_id → fetch details → fetch similar homes (comps).

Default backend port: 8081 (see `realtyinus/src/main/resources/application.yml`).

## 0) Start the backend

- Option A (script):
```powershell
.\scripts\start-realtyinus.bat
```

- Option B (manual Maven):
```powershell
cd .\realtyinus
.\mvnw.cmd spring-boot:run
```

Wait until Spring Boot reports Started and is listening on port 8081.

Sanity check (optional):
```powershell
(Invoke-RestMethod -TimeoutSec 30 -Uri 'http://localhost:8081/api/properties/search?postalCode=02171&limit=1' -Method GET) | ConvertTo-Json -Depth 4
```

## 1) One-click demo script

Run the interactive script and follow prompts (ZIP first, then property_id):
```powershell
.\scripts\demo-workflow.ps1
```
- It lists up to 5 properties for the given ZIP (status=for_sale),
- lets you select a property_id (or uses the first one by default),
- then fetches details and similar homes (limit=5),
- and saves JSON outputs into the current directory.

If your backend uses a different port:
```powershell
.\scripts\demo-workflow.ps1 -BaseUrl "http://localhost:8080"
```

## 2) Manual API calls (copy/paste)

Set a variable for convenience:
```powershell
$BASE = "http://localhost:8081"
```

Search properties by ZIP (top 5):
```powershell
Invoke-RestMethod -TimeoutSec 30 -Uri "$BASE/api/properties/search?postalCode=02171&status=for_sale&limit=5" -Method GET | ConvertTo-Json -Depth 6
```

Pick a property_id from the results, then fetch details:
```powershell
$propId = "3325825129"  # replace with an id from search
Invoke-RestMethod -TimeoutSec 30 -Uri "$BASE/api/properties/$propId" -Method GET | ConvertTo-Json -Depth 6
```

Fetch similar homes (comps):
```powershell
Invoke-RestMethod -TimeoutSec 30 -Uri "$BASE/api/properties/$propId/similar?limit=5" -Method GET | ConvertTo-Json -Depth 6
```

Fetch investment data (optional):
```powershell
Invoke-RestMethod -TimeoutSec 60 -Uri "$BASE/api/properties/$propId/investment-data" -Method GET | ConvertTo-Json -Depth 7
```

## Notes
- Market/rate data can be empty from upstream; backend returns conservative fallbacks when needed.
- If port 8081 is busy, change `server.port` in `application.yml`, restart the backend, and adjust `$BASE`.
- For a PDF/JSON investment report flow, see `docs/FRONTEND_HANDOFF.md` and `realtyinus/sample-report-request.json`.
