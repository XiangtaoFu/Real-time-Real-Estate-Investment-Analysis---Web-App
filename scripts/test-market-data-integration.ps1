# Test Market Data API Integration
Write-Host "Testing Market Data API Integration..." -ForegroundColor Cyan

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/api/properties/3325825129/investment-data" -Method GET
    $data = $response.Content | ConvertFrom-Json
    
    Write-Host "`n=== Market Data Results ===" -ForegroundColor Green
    Write-Host "Property ID: $($data.propertyInfo.propertyId)"
    Write-Host "Address: $($data.propertyInfo.address)"
    Write-Host "`nMarket Data (Real-time from APIs):" -ForegroundColor Yellow
    Write-Host "  - Current Mortgage Rate: $($data.marketData.currentMortgageRate)%"
    Write-Host "  - Estimated Monthly Rent: `$$($data.marketData.estimatedMonthlyRent)"
    Write-Host "  - Property Tax Rate: $($data.marketData.propertyTaxRate)%"
    Write-Host "  - Average HOA: `$$($data.marketData.averageHoa)"
    
    # Save full response
    $data | ConvertTo-Json -Depth 10 | Out-File "market-data-test-result.json" -Encoding UTF8
    Write-Host "`nFull response saved to: market-data-test-result.json" -ForegroundColor Cyan
    
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host "Make sure the service is running on port 8081" -ForegroundColor Yellow
}
