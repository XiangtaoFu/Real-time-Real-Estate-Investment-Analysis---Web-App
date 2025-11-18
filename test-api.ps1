Write-Host "Waiting for service to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

Write-Host "`nTesting Market Data API Integration..." -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/properties/3325825129/investment-data" -Method GET
    
    Write-Host "`n=== SUCCESS ===" -ForegroundColor Green
    Write-Host "Property: $($response.propertyInfo.address)" -ForegroundColor White
    Write-Host "`nMarket Data (Real-time):" -ForegroundColor Yellow
    Write-Host "  Mortgage Rate: $($response.marketData.currentMortgageRate)%" -ForegroundColor Cyan
    Write-Host "  Estimated Rent: `$$($response.marketData.estimatedMonthlyRent)" -ForegroundColor Cyan
    Write-Host "  Property Tax: $($response.marketData.propertyTaxRate)%" -ForegroundColor Cyan
    Write-Host "  Average HOA: `$$($response.marketData.averageHoa)" -ForegroundColor Cyan
    
    Write-Host "`nSaving full response..." -ForegroundColor Yellow
    $response | ConvertTo-Json -Depth 10 | Out-File "market-data-result.json" -Encoding UTF8
    Write-Host "Saved to: market-data-result.json" -ForegroundColor Green
    
} catch {
    Write-Host "`n=== FAILED ===" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nPress any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
