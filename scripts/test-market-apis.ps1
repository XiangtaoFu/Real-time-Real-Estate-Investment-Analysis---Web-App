# Test Market Data API Integration
Write-Host "Testing Market Data API Integration..." -ForegroundColor Cyan
Write-Host "Waiting 10 seconds for service to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

try {
    Write-Host "`n fetch data..." -ForegroundColor Cyan
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/properties/3325825129/investment-data" -Method GET
    
    Write-Host "SUCCESS! Market data integrated." -ForegroundColor Green
    
    $response | ConvertTo-Json -Depth 10 | Out-File "test-market-data-integration.json"
    Write-Host "`nSaved to: test-market-data-integration.json" -ForegroundColor Cyan
    
    if ($response.marketData) {
        Write-Host "`n=== MARKET DATA RETRIEVED ===" -ForegroundColor Yellow
        Write-Host "Mortgage Rate: $($response.marketData.currentMortgageRate)%" -ForegroundColor White
        Write-Host "Estimated Rent: `$$($response.marketData.estimatedMonthlyRent)" -ForegroundColor White
        Write-Host "Average HOA: `$$($response.marketData.averageHoa)" -ForegroundColor White
        Write-Host "Tax Rate: $($response.marketData.propertyTaxRate)%" -ForegroundColor White
    }
    
} catch {
    Write-Host "`nFAILED: $($_.Exception.Message)" -ForegroundColor Red
}
