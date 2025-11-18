# Demo Workflow Script: Search by ZIP → pick property_id → show details & similar comps
# Usage: Right-click and Run with PowerShell, or run from PowerShell: .\scripts\demo-workflow.ps1

param(
    [string]$BaseUrl = "http://localhost:8081",
    # Optional: provide ZIP to avoid prompt
    [string]$Zip,
    # Optional: provide propertyId to avoid prompt; if omitted and -NonInteractive is set, uses the first result
    [string]$PropertyId,
    # Optional: run without prompts; will use defaults (first listing) when possible
    [switch]$NonInteractive
)

Write-Host ("=" * 70) -ForegroundColor Gray
Write-Host "RealtyInUS Demo: ZIP → List → Detail → Similar" -ForegroundColor Cyan
Write-Host ("=" * 70) -ForegroundColor Gray

# 0) Quick readiness probe
try {
    $null = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Method Head -Uri $BaseUrl
} catch {
    Write-Host "Hint: The service may not be running or the port differs. Start it with: .\\scripts\\start-realtyinus.bat" -ForegroundColor Yellow
    Write-Host "Or pass -BaseUrl with the correct port, e.g., http://localhost:8081" -ForegroundColor Yellow
}

# 1) Ask ZIP code (or use provided)
if (-not $Zip) {
    $Zip = if ($NonInteractive) { "02171" } else { Read-Host "Enter ZIP code (e.g., 02171 or 02215)" }
}
if (-not $Zip) { Write-Host "No ZIP entered. Exit." -ForegroundColor Red; exit 1 }

# 2) Search properties
$searchUrl = "$BaseUrl/api/properties/search?postalCode=$Zip&status=for_sale&limit=5"
Write-Host "\n[1] Search properties: $searchUrl" -ForegroundColor Cyan

try {
    $search = Invoke-RestMethod -TimeoutSec 30 -Uri $searchUrl -Method GET
} catch {
    Write-Host "Search request failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

if (-not $search -or -not $search.properties -or $search.properties.Count -eq 0) {
    Write-Host "No listings found. Try another ZIP or retry later." -ForegroundColor Yellow
    exit 0
}

Write-Host "\nTop 5 listings:" -ForegroundColor Green
$search.properties |
  Select-Object -First 5 @{N='propertyId';E={$_.propertyId}}, @{N='address';E={$_.address}}, city, state, @{N='listPrice';E={$_.listPrice}}, beds, baths, sqft |
  Format-Table -AutoSize

$defaultPropId = ($search.properties | Select-Object -First 1).propertyId
if (-not $PropertyId) {
    if ($NonInteractive) {
        $PropertyId = $defaultPropId
    } else {
        $propIdInput = Read-Host "\nEnter property_id (press Enter to use default $defaultPropId)"
        $PropertyId = if ([string]::IsNullOrWhiteSpace($propIdInput)) { $defaultPropId } else { $propIdInput.Trim() }
    }
}

if (-not $PropertyId) {
    Write-Host "No property_id selected. Exit." -ForegroundColor Red
    exit 1
}

# 3) Fetch detail
$detailUrl = "$BaseUrl/api/properties/$PropertyId"
Write-Host "\n[2] Fetch detail: $detailUrl" -ForegroundColor Cyan
try {
    $detail = Invoke-RestMethod -TimeoutSec 30 -Uri $detailUrl -Method GET
} catch {
    Write-Host "Detail request failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "\nDetail - key fields:" -ForegroundColor Green
if ($detail -and $detail.propertyInfo) {
    $detail.propertyInfo | Select-Object propertyId, address, city, state, postalCode, listPrice, beds, baths, sqft, status | Format-List
} else {
    $detail | ConvertTo-Json -Depth 6 | Write-Output
}

# 4) Fetch similar comps
$similarUrl = "$BaseUrl/api/properties/$PropertyId/similar?limit=5"
Write-Host "\n[3] Similar homes (comps): $similarUrl" -ForegroundColor Cyan

try {
    $similar = Invoke-RestMethod -TimeoutSec 30 -Uri $similarUrl -Method GET
} catch {
    Write-Host "Similar homes request failed: $($_.Exception.Message)" -ForegroundColor Red
    $similar = $null
}

if ($similar) {
    $similar | Select-Object -First 5 @{N='propertyId';E={$_.propertyId}}, @{N='address';E={$_.address}}, @{N='price';E={$_.price}}, beds, baths, sqft, status |
      Format-Table -AutoSize
} else {
    Write-Host "No similar homes returned or API currently unavailable." -ForegroundColor Yellow
}

# 5) Save artifacts for review
$searchPath  = Join-Path -Path (Get-Location) -ChildPath ("search_" + $Zip + ".json")
$detailPath  = Join-Path -Path (Get-Location) -ChildPath ("detail_" + $PropertyId + ".json")
$similarPath = Join-Path -Path (Get-Location) -ChildPath ("similar_" + $PropertyId + ".json")

try { $search  | ConvertTo-Json -Depth 8 | Out-File -FilePath $searchPath  -Encoding UTF8 } catch {}
try { $detail  | ConvertTo-Json -Depth 8 | Out-File -FilePath $detailPath  -Encoding UTF8 } catch {}
try { if ($similar) { $similar | ConvertTo-Json -Depth 8 | Out-File -FilePath $similarPath -Encoding UTF8 } } catch {}

Write-Host "\nSaved output files:" -ForegroundColor Green
Write-Host " - $searchPath" -ForegroundColor Gray
Write-Host " - $detailPath" -ForegroundColor Gray
if ($similar) { Write-Host " - $similarPath" -ForegroundColor Gray }

Write-Host "\nDemo completed." -ForegroundColor Cyan
