# End-to-End Demo Guide (English)

This short guide shows how to run the backend and demonstrate the full workflow:
Search by ZIP → pick a property_id → fetch details → fetch similar homes (comps).

# End-to-End Demo Guide (English)

This short guide shows how to run the backend and demonstrate the full workflow:
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

Non-interactive run (skip prompts) and specify a ZIP:
```powershell
 .\scripts\demo-workflow.ps1 -Zip "02171" -NonInteractive
```
You can also pre-select a property id:
```powershell
 .\scripts\demo-workflow.ps1 -Zip "02171" -PropertyId "3247828734" -NonInteractive
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
- For a PDF/JSON investment report flow, see section below and `docs/FRONTEND_HANDOFF.md` plus `realtyinus/sample-report-request.json`.

> New demos available:
- Browser XHR sample (RapidAPI direct): `docs/demos/rapidapi-xhr-demo.html`
- PowerShell report demo: `scripts/generate-report-demo.ps1`

## 3) Generate Investment Report (JSON & PDF)

## 4) Quick demos (English)

### 4.1 RapidAPI XHR (browser-side)
- Open `docs/demos/rapidapi-xhr-demo.html` in a browser.
- Paste your RapidAPI Key, set ZIP and status, click Search.
- Pick a `property_id`, then click Detail and Similar.
- Note: This exposes your key in the browser; for production use the backend as a proxy instead.

### 4.2 Report generation demo (PowerShell)
Generate a JSON and PDF report from a request payload:
```powershell
./scripts/generate-report-demo.ps1 -BaseUrl "http://localhost:8081" -PropertyId "<optional>"

# With a custom request file and output directory
./scripts/generate-report-demo.ps1 -RequestFile .\realtyinus\sample-report-request.json -OutDir .\realtyinus\output
```
Outputs are written to `realtyinus/output`, e.g. `report_<propertyId>.json` and `report_<propertyId>.pdf`.

You can produce a structured investment report once you have a propertyId and have decided whether to use market rent/rate or custom values.

### 3.1 JSON Report

1. Edit `realtyinus/sample-report-request.json` (replace propertyId, address, and any user overrides).
2. Call the JSON endpoint:
```powershell
Invoke-RestMethod -TimeoutSec 60 `
	-Uri "$BASE/api/report/generate" `
	-Method POST `
	-ContentType "application/json" `
	-InFile ".\realtyinus\sample-report-request.json" |
	ConvertTo-Json -Depth 10 | Out-File "report_$propId.json" -Encoding UTF8
```
3. Inspect `report_$propId.json` for sections: `propertyInfo`, `analysis.yearOne`, `analysis.projection[]`, `exit`, `dataQuality`, `assumptions`, `warnings`.

### 3.2 PDF Report

Generates a minimal PDF (layout/table enhancements planned):
```powershell
Invoke-WebRequest -TimeoutSec 60 `
	-Uri "$BASE/api/report/generate/pdf" `
	-Method POST `
	-ContentType "application/json" `
	-InFile ".\realtyinus\sample-report-request.json" `
	-OutFile ".\realtyinus\output\report_${propId}.pdf"
```
Open `.\realtyinus\output\report_${propId}.pdf` after success.

### 3.3 Common Adjustments
- Market rent: `useMarketRent: true` (omit `actualMonthlyRent`).
- Custom rent: `useMarketRent: false` + provide `actualMonthlyRent`.
- Market rate: `useMarketRate: true`.
- Custom rate: `useMarketRate: false` + provide `mortgageRate`.
- Closing costs: set `includeClosingCosts: true` to include them in analysis.
- Percent fields are decimals (e.g., 0.20 not 20).

### 3.4 Example Payload
```json
{
	"apiData": {
		"propertyId": "3325825129",
		"address": "30 Fenway Unit 5",
		"city": "Boston",
		"state": "MA",
		"zip": "02215",
		"listPrice": 849000,
		"estimate": 1191700,
		"beds": 0,
		"baths": 1,
		"sqft": 1476,
		"propertyType": "condos",
		"status": "for_sale"
	},
	"marketData": {
		"estimatedMonthlyRent": 3900,
		"currentMortgageRate": 0.065
	},
	"userInput": {
		"offerPrice": 849000,
		"downPaymentPercent": 0.20,
		"loanTermYears": 30,
		"interestOnlyYears": 0,
		"holdYears": 10,
		"expectedAppreciation": 0.04
	},
	"useMarketRent": true,
	"useMarketRate": true,
	"includeClosingCosts": false
}
```
Save, then run the JSON report command to preview results.
