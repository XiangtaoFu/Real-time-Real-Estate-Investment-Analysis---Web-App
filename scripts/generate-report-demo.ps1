param(
  [string]$BaseUrl = "http://localhost:8081",
  [string]$RequestFile,
  [string]$PropertyId,
  [string]$OutDir
)

$ErrorActionPreference = 'Stop'

function Write-Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Ok($msg) { Write-Host "[OK]   $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Err($msg) { Write-Host "[ERR]  $msg" -ForegroundColor Red }

try {
  $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
  if (-not $RequestFile) {
    $RequestFile = Join-Path $scriptRoot "..\realtyinus\sample-report-request.json"
  }
  if (-not $OutDir) {
    $OutDir = Join-Path $scriptRoot "..\realtyinus\output"
  }
  $RequestFile = (Resolve-Path $RequestFile).Path
  New-Item -ItemType Directory -Path $OutDir -Force | Out-Null

  Write-Info "BaseUrl: $BaseUrl"
  Write-Info "RequestFile: $RequestFile"
  Write-Info "OutDir: $OutDir"

  if (-not (Test-Path $RequestFile)) { throw "Request file not found: $RequestFile" }

  $req = Get-Content $RequestFile -Raw | ConvertFrom-Json

  # If PropertyId not provided, try to infer from last_propId.txt or request payload
  if (-not $PropertyId) {
    $lastPidFile = Join-Path $OutDir "last_propId.txt"
    if (Test-Path $lastPidFile) {
      $PropertyId = (Get-Content $lastPidFile -Raw).Trim()
      if ($PropertyId) { Write-Info "Using PropertyId from last_propId.txt: $PropertyId" }
    }
  }
  if (-not $PropertyId -and $req.apiData -and $req.apiData.propertyId) {
    $PropertyId = "$($req.apiData.propertyId)"
    if ($PropertyId) { Write-Info "Using PropertyId from request payload (apiData.propertyId): $PropertyId" }
  }
  if ($PropertyId) {
    if (-not $req.apiData) { $req | Add-Member -NotePropertyName apiData -NotePropertyValue (@{}) -Force }
    $req.apiData.propertyId = $PropertyId
  }

  # Compute output filenames
  $pidSafe = if ($PropertyId) { $PropertyId } elseif ($req.apiData -and $req.apiData.propertyId) { $req.apiData.propertyId } else { (Get-Date -Format 'yyyyMMddHHmmss') }
  $reqOut = Join-Path $OutDir ("report_request_{0}.json" -f $pidSafe)
  $jsonOut = Join-Path $OutDir ("report_{0}.json" -f $pidSafe)
  $pdfOut = Join-Path $OutDir ("report_{0}.pdf" -f $pidSafe)

  # Persist the final request for traceability
  ($req | ConvertTo-Json -Depth 20) | Out-File -FilePath $reqOut -Encoding UTF8
  Write-Ok "Wrote request: $reqOut"

  # POST JSON report
  $jsonUri = "$BaseUrl/api/report/generate"
  Write-Info "POST $jsonUri"
  try {
    $resp = Invoke-RestMethod -Method Post -Uri $jsonUri -ContentType 'application/json' -Body ($req | ConvertTo-Json -Depth 20)
    ($resp | ConvertTo-Json -Depth 20) | Out-File -FilePath $jsonOut -Encoding UTF8
    Write-Ok "Saved JSON report: $jsonOut"
  } catch {
    Write-Err ("JSON report request failed: " + $_)
    if ($_.Exception.Response -and $_.Exception.Response.Content) {
      Write-Err ("Server said: " + $_.Exception.Response.Content)
    }
    throw
  }

  # POST PDF report (binary)
  $pdfUri = "$BaseUrl/api/report/generate/pdf"
  Write-Info "POST $pdfUri"
  try {
    Invoke-WebRequest -UseBasicParsing -Method Post -Uri $pdfUri -ContentType 'application/json' -Body ($req | ConvertTo-Json -Depth 20) -OutFile $pdfOut
  } catch {
    Write-Err ("PDF report request failed: " + $_)
    if ($_.Exception.Response -and $_.Exception.Response.Content) {
      Write-Err ("Server said: " + $_.Exception.Response.Content)
    }
    throw
  }
  if ((Test-Path $pdfOut) -and ((Get-Item $pdfOut).Length -gt 0)) {
    Write-Ok "Saved PDF report: $pdfOut"
  } else {
    Write-Warn "PDF not generated or empty: $pdfOut"
  }

  Write-Host ""; Write-Ok "Done."
}
catch {
  Write-Err $_
  exit 1
}
