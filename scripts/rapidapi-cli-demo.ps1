param(
  [string]$RapidApiKey,
  [ValidatePattern('^\d{5}(-\d{4})?$')]
  [string]$Zip,
  [string]$PropertyId,
  [switch]$OnlyDetail,
  [switch]$UseBackend,
  [ValidateSet('for_sale','for_rent','sold')][string]$Status = 'for_sale',
  [int]$Limit = 5,
  [string]$ApiHost = 'realty-in-us.p.rapidapi.com',
  [string]$BaseUrl = 'http://localhost:8081',
  [switch]$RunBackendFlow,
  [int]$BackendSimilarLimit = 5,
  [string]$OutDir,
  [switch]$NonInteractive
)

$ErrorActionPreference = 'Stop'

function Write-Info($m){ Write-Host "[INFO] $m" -ForegroundColor Cyan }
function Write-Ok($m){ Write-Host "[OK ] $m" -ForegroundColor Green }
function Write-Warn($m){ Write-Host "[WARN] $m" -ForegroundColor Yellow }
function Write-Err($m){ Write-Host "[ERR] $m" -ForegroundColor Red }

try {
  # Embedded RapidAPI key for demo convenience (override via -RapidApiKey or $env:RAPIDAPI_KEY)
  $DEFAULT_RAPIDAPI_KEY = 'cdff686e3dmsh63e57fae45f21f1p113364jsn85fbadde0225'

  $root = Split-Path -Parent $MyInvocation.MyCommand.Path
  if (-not $OutDir) { $OutDir = Join-Path $root "..\realtyinus\output" }
  New-Item -ItemType Directory -Path $OutDir -Force | Out-Null

  # Resolve API key priority: param > env > embedded default > prompt
  if (-not $RapidApiKey) {
    if ($env:RAPIDAPI_KEY) { $RapidApiKey = $env:RAPIDAPI_KEY }
    else { $RapidApiKey = $DEFAULT_RAPIDAPI_KEY }
  }
  if (-not $RapidApiKey -or $RapidApiKey.Trim().Length -eq 0) {
    if ($NonInteractive) { throw 'Missing RapidAPI Key' }
    $RapidApiKey = Read-Host -Prompt 'Enter RapidAPI Key'
  }
  # If PropertyId not provided, we need a ZIP to run the search
  if (-not $PropertyId) {
    if (-not $Zip) {
      if ($NonInteractive) { throw 'Missing -Zip (ZIP code required when -PropertyId is not provided)' }
      $Zip = Read-Host -Prompt 'Enter ZIP code (e.g. 02171)'
    }
  }
  if (-not $Limit -or $Limit -le 0) { $Limit = 5 }

  # Build headers used for RapidAPI calls
  $headers = @{ 'Content-Type'='application/json'; 'X-RapidAPI-Key'=$RapidApiKey; 'X-RapidAPI-Host'=$ApiHost }

  $ts = Get-Date -Format 'yyyyMMdd_HHmmss'
  $pickedId = $PropertyId

  if (-not $pickedId) {
    # Run the search first to get a property_id
    Write-Info "Searching properties: ZIP=$Zip, status=$Status, limit=$Limit"
    $listUri = "https://$ApiHost/properties/v3/list"
    $payload = @{ postal_code=$Zip; status=$Status; limit=$Limit; sort=@{ direction='desc'; field='list_date' } } | ConvertTo-Json

    $listResp = Invoke-RestMethod -Method Post -Uri $listUri -Headers $headers -Body $payload -TimeoutSec 90
    $listOut = Join-Path $OutDir ("raw_search_{0}_{1}.json" -f $Zip,$ts)
    ($listResp | ConvertTo-Json -Depth 20) | Out-File -FilePath $listOut -Encoding UTF8
    Write-Ok "Saved: $listOut"

    $results = $listResp.data.home_search.results
    if (-not $results -or $results.Count -eq 0) { throw 'No results returned for the given ZIP.' }

    Write-Host ''
    Write-Host 'Top results:' -ForegroundColor White
    $rows = @()
    $i = 0
    foreach($r in $results){
      $i++
      if ($i -gt $Limit) { break }
      $propIdVar = $r.property_id
      $addr = $r.location.address.line
      $city = $r.location.address.city
      $st = $r.location.address.state_code
      $zipc = $r.location.address.postal_code
      $price = if ($r.list_price) { [decimal]$r.list_price } elseif ($r.price){ [decimal]$r.price } else { $null }
      $rows += [PSCustomObject]@{
        Index = $i
        PropertyId = $propIdVar
        Address = ("{0}, {1}, {2} {3}" -f $addr,$city,$st,$zipc)
        ListPrice = if ($price){ ("${0:N0}" -f $price) } else { '' }
      }
    }
    $rows | Format-Table -AutoSize | Out-Host

    if ($NonInteractive) {
      $pickedId = $results[0].property_id
      Write-Info "Picked first property_id: $pickedId"
    } else {
      $choice = Read-Host -Prompt 'Enter number (Index) to select, or paste a property_id'
      if ($choice -match '^[0-9]+$'){
        $idx = [int]$choice
        if ($idx -ge 1 -and $idx -le $rows.Count){ $pickedId = $rows[$idx-1].PropertyId }
        else { throw "Index out of range: $idx" }
      } else {
        $pickedId = $choice
      }
    }
    if (-not $pickedId){ throw 'No property selected' }
  }

  # Save last_propId
  $lastPropFile = Join-Path $OutDir 'last_propId.txt'
  $pickedId | Out-File -FilePath $lastPropFile -Encoding ASCII

  if (-not $UseBackend) {
    # RapidAPI Detail
    $detailUri = "https://$ApiHost/properties/v3/detail?property_id=$pickedId"
    Write-Info "GET detail: $detailUri"
    $detail = Invoke-RestMethod -Method Get -Uri $detailUri -Headers $headers -TimeoutSec 90
    $detailOut = Join-Path $OutDir ("detail_{0}_{1}.json" -f $pickedId,$ts)
    ($detail | ConvertTo-Json -Depth 20) | Out-File -FilePath $detailOut -Encoding UTF8
    Write-Ok "Saved: $detailOut"

    if (-not $OnlyDetail) {
      # RapidAPI Similar
      $similarUri = "https://$ApiHost/properties/v3/list-similar-homes?property_id=$pickedId&limit=10&status=for_sale"
      Write-Info "GET similar: $similarUri"
      $similar = Invoke-RestMethod -Method Get -Uri $similarUri -Headers $headers -TimeoutSec 90
      $similarOut = Join-Path $OutDir ("similar_{0}_{1}.json" -f $pickedId,$ts)
      ($similar | ConvertTo-Json -Depth 20) | Out-File -FilePath $similarOut -Encoding UTF8
      Write-Ok "Saved: $similarOut"
    }
  } else {
    # Backend Detail
    $beDetailUri = "$BaseUrl/api/properties/$pickedId"
    Write-Info "GET backend detail: $beDetailUri"
    $beDetail = Invoke-RestMethod -Method Get -Uri $beDetailUri -TimeoutSec 90
    $beDetailOut = Join-Path $OutDir ("backend_detail_{0}_{1}.json" -f $pickedId,$ts)
    ($beDetail | ConvertTo-Json -Depth 20) | Out-File -FilePath $beDetailOut -Encoding UTF8
    Write-Ok "Saved: $beDetailOut"

    if (-not $OnlyDetail) {
      $beSimilarUri = "$BaseUrl/api/properties/$pickedId/similar?limit=$BackendSimilarLimit"
      Write-Info "GET backend similar: $beSimilarUri"
      $beSimilar = Invoke-RestMethod -Method Get -Uri $beSimilarUri -TimeoutSec 90
      $beSimilarOut = Join-Path $OutDir ("backend_similar_{0}_{1}.json" -f $pickedId,$ts)
      ($beSimilar | ConvertTo-Json -Depth 20) | Out-File -FilePath $beSimilarOut -Encoding UTF8
      Write-Ok "Saved: $beSimilarOut"
    }
  }

  if ($RunBackendFlow -and -not $UseBackend) {
    Write-Host ''
    Write-Info "Running backend flow on $BaseUrl using propertyId=$pickedId ..."
    try {
      # Backend: detail
      $beDetailUri = "$BaseUrl/api/properties/$pickedId"
      Write-Info "GET backend detail: $beDetailUri"
      $beDetail = Invoke-RestMethod -Method Get -Uri $beDetailUri -TimeoutSec 90
      $beDetailOut = Join-Path $OutDir ("backend_detail_{0}_{1}.json" -f $pickedId,$ts)
      ($beDetail | ConvertTo-Json -Depth 20) | Out-File -FilePath $beDetailOut -Encoding UTF8
      Write-Ok "Saved: $beDetailOut"

      # Backend: similar
      $beSimilarUri = "$BaseUrl/api/properties/$pickedId/similar?limit=$BackendSimilarLimit"
      Write-Info "GET backend similar: $beSimilarUri"
      $beSimilar = Invoke-RestMethod -Method Get -Uri $beSimilarUri -TimeoutSec 90
      $beSimilarOut = Join-Path $OutDir ("backend_similar_{0}_{1}.json" -f $pickedId,$ts)
      ($beSimilar | ConvertTo-Json -Depth 20) | Out-File -FilePath $beSimilarOut -Encoding UTF8
      Write-Ok "Saved: $beSimilarOut"

      # Backend: investment-data
      $beInvestUri = "$BaseUrl/api/properties/$pickedId/investment-data"
      Write-Info "GET backend investment-data: $beInvestUri"
      $beInvest = Invoke-RestMethod -Method Get -Uri $beInvestUri -TimeoutSec 120
      $beInvestOut = Join-Path $OutDir ("backend_investment_data_{0}_{1}.json" -f $pickedId,$ts)
      ($beInvest | ConvertTo-Json -Depth 20) | Out-File -FilePath $beInvestOut -Encoding UTF8
      Write-Ok "Saved: $beInvestOut"

      # Generate report JSON + PDF
      $genScript = Join-Path $root 'generate-report-demo.ps1'
      if (Test-Path $genScript) {
        Write-Info "Generating report via $genScript ..."
        & $genScript -BaseUrl $BaseUrl -PropertyId $pickedId | Out-Host
        Write-Ok "Report generation invoked"
      } else {
        Write-Warn "Report script not found: $genScript"
      }
    }
    catch {
      Write-Warn "Backend flow encountered an issue: $_"
    }
  }

  Write-Host ''
  Write-Ok "Done. Files saved to: $OutDir"
}
catch {
  Write-Err $_
  exit 1
}
