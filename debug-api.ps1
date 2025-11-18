Write-Host "Testing Market Data API Integration - Debug Mode" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Gray

try {
    Write-Host "`nStep 1: Testing basic property detail..." -ForegroundColor Yellow
    $basic = Invoke-RestMethod -Uri "http://localhost:8081/api/properties/3325825129" -Method GET
    Write-Host "  ✓ Property ID: $($basic.propertyInfo.propertyId)" -ForegroundColor Green
    Write-Host "  ✓ Address: $($basic.propertyInfo.address)" -ForegroundColor Green
    Write-Host "  ✓ Zip Code: $($basic.propertyInfo.postalCode)" -ForegroundColor Green
    
    Write-Host "`nStep 2: Testing investment-data endpoint..." -ForegroundColor Yellow
    try {
        $full = Invoke-WebRequest -Uri "http://localhost:8081/api/properties/3325825129/investment-data" -Method GET
        
        if ($full.StatusCode -eq 200) {
            $data = $full.Content | ConvertFrom-Json
            
            Write-Host "  ✓ SUCCESS! Status: $($full.StatusCode)" -ForegroundColor Green
            Write-Host "`nMarket Data Retrieved:" -ForegroundColor Cyan
            Write-Host "  - Mortgage Rate: $($data.marketData.currentMortgageRate)%" -ForegroundColor White
            Write-Host "  - Estimated Rent: `$$($data.marketData.estimatedMonthlyRent)" -ForegroundColor White
            Write-Host "  - Tax Rate: $($data.marketData.propertyTaxRate)%" -ForegroundColor White
            Write-Host "  - Average HOA: `$$($data.marketData.averageHoa)" -ForegroundColor White
            
            Write-Host "`nSaving to file..." -ForegroundColor Yellow
            $data | ConvertTo-Json -Depth 10 | Out-File "market-data-success.json" -Encoding UTF8
            Write-Host "  ✓ Saved to: market-data-success.json" -ForegroundColor Green
        }
        
    } catch {
        Write-Host "  ✗ FAILED!" -ForegroundColor Red
        Write-Host "`nError Details:" -ForegroundColor Yellow
        Write-Host "  Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        Write-Host "  Message: $($_.Exception.Message)" -ForegroundColor Red
        
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "`nServer Response:" -ForegroundColor Yellow
            Write-Host $responseBody -ForegroundColor Gray
            $responseBody | Out-File "error-response.txt" -Encoding UTF8
            Write-Host "`n  Saved to: error-response.txt" -ForegroundColor Cyan
        }
    }
    
} catch {
    Write-Host "`n✗ Basic test failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n" + ("=" * 60) -ForegroundColor Gray
Write-Host "Test complete. Press any key to exit..." -ForegroundColor Cyan
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
