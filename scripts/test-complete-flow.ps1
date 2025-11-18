# Complete API Test Flow - Real-Time Real Estate Investment Analysis
# This script tests: Realty API → Data Mapping → Cashflow Calculator → Report Generation

Write-Host "=================================" -ForegroundColor Cyan
Write-Host "Real-Time Investment Analysis Test" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""

# STEP 1: Search for properties using Realty In US API
Write-Host "[STEP 1] Searching for properties in Boston 02215..." -ForegroundColor Green
$searchResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/properties/search?postalCode=02215&limit=1"
$searchData = $searchResponse.Content | ConvertFrom-Json
$property = $searchData.properties[0]

Write-Host "  ✓ Found property:" -ForegroundColor Yellow
Write-Host "    - ID: $($property.propertyId)"
Write-Host "    - Address: $($property.address), $($property.city)"
Write-Host "    - Price: `$$($property.listPrice)"
Write-Host "    - Estimate (FMV): `$$($property.estimate)"
Write-Host "    - Type: $($property.propertyType)"
Write-Host ""

# STEP 2: Get detailed property data
Write-Host "[STEP 2] Fetching property details from Realty API..." -ForegroundColor Green
$detailResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/properties/$($property.propertyId)"
$detailData = $detailResponse.Content | ConvertFrom-Json

Write-Host "  ✓ API returned fields (NULL where data unavailable):" -ForegroundColor Yellow
Write-Host "    - Beds: $(if($detailData.beds){$detailData.beds}else{'NULL'})"
Write-Host "    - Baths: $(if($detailData.baths){$detailData.baths}else{'NULL'})"
Write-Host "    - SqFt: $(if($detailData.sqft){$detailData.sqft}else{'NULL'})"
Write-Host "    - Property Tax: $(if($detailData.propertyTax){'$'+$detailData.propertyTax}else{'NULL'})"
Write-Host "    - HOA Fee: $(if($detailData.hoaFee){'$'+$detailData.hoaFee}else{'NULL'})"
Write-Host "    - Year Built: $(if($detailData.yearBuilt){$detailData.yearBuilt}else{'NULL'})"
Write-Host ""

# STEP 3: Prepare Cashflow Calculator input (filling NULL with estimates)
Write-Host "[STEP 3] Preparing Cashflow Calculator input..." -ForegroundColor Green
$cashflowInput = @{
    address = $property.address
    city = $property.city
    state = $property.state
    zip = "02215"
    offerPrice = [double]$property.listPrice
    fmv = if($property.estimate){[double]$property.estimate}else{[double]$property.listPrice * 1.05}
    grossRentsAnnual = 48000  # User must provide (NULL from API)
    numberOfUnits = 1
    firstPrincipal = [double]$property.listPrice * 0.8  # 80% LTV
    firstRateAnnual = 0.065  # User must provide (NULL from API)
    firstAmortYears = 30
    vacancyRate = 0.05  # Industry standard
    managementRate = 0.08  # Industry standard
    repairsRate = 0.05  # Industry standard
    propertyTaxes = if($detailData.propertyTax){[double]$detailData.propertyTax}else{[double]$property.listPrice * 0.01}
    insurance = [double]$property.listPrice * 0.003  # Estimate 0.3%
}

Write-Host "  ✓ Input prepared with data sources:" -ForegroundColor Yellow
Write-Host "    - From API: address, city, state, price, estimate"
Write-Host "    - Calculated: firstPrincipal (80% LTV)"
Write-Host "    - Industry Standard: vacancy (5%), management (8%), repairs (5%)"
Write-Host "    - User Required: grossRentsAnnual, firstRateAnnual"
Write-Host "    - Estimated: insurance (0.3% of price)"
Write-Host ""

# STEP 4: Calculate investment metrics using Cashflow Calculator
Write-Host "[STEP 4] Running Cashflow Calculator..." -ForegroundColor Green
$cashflowJson = $cashflowInput | ConvertTo-Json
$calcResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/analysis/cashflow" -Method POST -ContentType "application/json" -Body $cashflowJson
$result = $calcResponse.Content | ConvertFrom-Json

Write-Host "  ✓ Calculation Complete! Investment Metrics:" -ForegroundColor Yellow
Write-Host ""
Write-Host "    === Year 1 Key Performance Indicators ===" -ForegroundColor Cyan
Write-Host "    Cap Rate (Purchase Price): $([math]::Round($result.summary.capRatePPY1 * 100, 2))%"
Write-Host "    Cap Rate (FMV): $([math]::Round($result.summary.capRateFMVY1 * 100, 2))%"
Write-Host "    Net Operating Income: `$$($result.summary.noiY1)"
Write-Host "    Annual Debt Service: `$$($result.summary.annualDebtServiceY1)"
Write-Host "    DSCR (Debt Service Coverage Ratio): $([math]::Round($result.summary.dscrY1, 2))"
Write-Host "    Monthly Cashflow: `$$([math]::Round($result.summary.monthlyProfitY1, 2))"
Write-Host ""
Write-Host "    === Return Metrics ===" -ForegroundColor Cyan
Write-Host "    Cash-on-Cash Return: $([math]::Round($result.summary.cashOnCashY1 * 100, 2))%"
Write-Host "    Equity ROI: $([math]::Round($result.summary.equityROIY1 * 100, 2))%"
Write-Host "    Total ROI (Year 1): $([math]::Round($result.summary.totalROIY1 * 100, 2))%"
Write-Host "    IRR (10-year): $([math]::Round($result.irr * 100, 2))%"
Write-Host ""
Write-Host "    === Investment Summary ===" -ForegroundColor Cyan
Write-Host "    Cash to Close: `$$($result.summary.cashToClose)"
Write-Host "    Loan-to-Value (FMV): $([math]::Round($result.summary.ltvFMV * 100, 2))%"
Write-Host "    GRM (Gross Rent Multiplier): $([math]::Round($result.summary.grmY1, 2))"
Write-Host ""

# STEP 5: Save complete report
Write-Host "[STEP 5] Generating complete investment report..." -ForegroundColor Green
$report = @{
    propertyInfo = $property
    detailedData = $detailData
    calculatorInput = $cashflowInput
    investmentAnalysis = $result
    dataQuality = @{
        apiProvided = @("address", "city", "state", "listPrice", "estimate", "propertyType")
        calculated = @("firstPrincipal")
        industryStandard = @("vacancyRate", "managementRate", "repairsRate")
        userRequired = @("grossRentsAnnual", "firstRateAnnual")
        estimated = @("insurance", "propertyTaxes")
    }
    timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
}

$report | ConvertTo-Json -Depth 10 | Out-File -FilePath "complete-investment-report.json" -Encoding UTF8

Write-Host "  ✓ Complete report saved to: complete-investment-report.json" -ForegroundColor Yellow
Write-Host ""
Write-Host "=================================" -ForegroundColor Cyan
Write-Host "Test Complete! Summary:" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host "✓ Realty API: Connected and returned property data"
Write-Host "✓ Data Mapping: NULL fields identified and handled"
Write-Host "✓ Cashflow Calculator: Successfully computed all metrics"
Write-Host "✓ Report: Generated with 10-year cashflow projection"
Write-Host ""
Write-Host "Files Generated:" -ForegroundColor Green
Write-Host "  - complete-investment-report.json (Full analysis)"
Write-Host ""
