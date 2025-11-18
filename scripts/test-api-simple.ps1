# Simple API Test Script
# Test RapidAPI Realty-in-US API

$apiKey = $env:RAPIDAPI_KEY
if (-not $apiKey) {
    Write-Error "RAPIDAPI_KEY environment variable not set. Please set it before running."
    exit 1
}
$apiHost = "realty-in-us.p.rapidapi.com"

Write-Host "================================="
Write-Host "API Connection Test Starting"
Write-Host "================================="
Write-Host ""

# Test 1: GET /properties/v3/detail
Write-Host "[Test 1] GET /properties/v3/detail"
try {
    $headers = @{
        "x-rapidapi-key" = $apiKey
        "x-rapidapi-host" = $apiHost
    }
    
    # v3/detail requires property_id parameter
    $propertyId = "9884614204"
    $url = "https://$apiHost/properties/v3/detail?property_id=$propertyId"
    
    Write-Host "Request URL: $url"
    
    $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers -TimeoutSec 20
    
    Write-Host "SUCCESS - API is working!" -ForegroundColor Green
    Write-Host "Response:"
    $response | ConvertTo-Json -Depth 2
    
} catch {
    Write-Host "FAILED" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "---------------------------------"
Write-Host ""

# Test 2: POST /properties/v3/list
Write-Host "[Test 2] POST /properties/v3/list"
try {
    $headers = @{
        "x-rapidapi-key" = $apiKey
        "x-rapidapi-host" = $apiHost
        "Content-Type" = "application/json"
    }
    
    $body = @{
        limit = 5
        offset = 0
        postal_code = "10001"
        status = @("for_sale")
        sort = @{
            direction = "desc"
            field = "list_date"
        }
    } | ConvertTo-Json
    
    $url = "https://$apiHost/properties/v3/list"
    
    Write-Host "Request URL: $url"
    
    $response = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -TimeoutSec 20
    
    Write-Host "SUCCESS - API is working!" -ForegroundColor Green
    
    if ($response.data.home_search) {
        $total = $response.data.home_search.total
        $count = $response.data.home_search.count
        Write-Host "Found $total properties, returned $count"
        
        if ($response.data.home_search.results) {
            Write-Host ""
            Write-Host "First 3 properties:"
            $index = 1
            foreach ($property in ($response.data.home_search.results | Select-Object -First 3)) {
                $propertyId = $property.property_id
                $address = $property.location.address.line
                $price = if ($property.list_price) { "$" + $property.list_price } else { "N/A" }
                
                Write-Host "  $index. ID: $propertyId, Address: $address, Price: $price"
                $index++
            }
        }
    }
    
} catch {
    Write-Host "FAILED" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "---------------------------------"
Write-Host ""

# Test 3: GET /finance/rates
Write-Host "[Test 3] GET /finance/rates"
try {
    $headers = @{
        "x-rapidapi-key" = $apiKey
        "x-rapidapi-host" = $apiHost
    }
    
    $url = "https://$apiHost/finance/rates"
    
    Write-Host "Request URL: $url"
    
    $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers -TimeoutSec 20
    
    Write-Host "SUCCESS - API is working!" -ForegroundColor Green
    Write-Host "Response:"
    $response | ConvertTo-Json -Depth 2
    
} catch {
    Write-Host "FAILED" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "---------------------------------"
Write-Host ""

# Test 4: GET /mortgage/v2/check-rates
Write-Host "[Test 4] GET /mortgage/v2/check-rates"
try {
    $headers = @{
        "x-rapidapi-key" = $apiKey
        "x-rapidapi-host" = $apiHost
    }
    
    $url = "https://$apiHost/mortgage/v2/check-rates?postal_code=10001"
    
    Write-Host "Request URL: $url"
    
    $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers -TimeoutSec 20
    
    Write-Host "SUCCESS - API is working!" -ForegroundColor Green
    Write-Host "Response:"
    $response | ConvertTo-Json -Depth 2
    
} catch {
    Write-Host "FAILED" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "================================="
Write-Host "All Tests Completed"
Write-Host "================================="

