# API 连接测试脚本
# 测试 RapidAPI Realty-in-US API 是否可用

$apiKey = "cdff686e3dmsh63e57fae45f21f1p113364jsn85fbadde0225"
$apiHost = "realty-in-us.p.rapidapi.com"

Write-Host "=================================" -ForegroundColor Cyan
Write-Host "API 连接测试开始" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""

# 测试1: GET /properties/v3/detail
Write-Host "【测试1】 GET /properties/v3/detail" -ForegroundColor Yellow
try {
    $headers = @{
        "x-rapidapi-key" = $apiKey
        "x-rapidapi-host" = $apiHost
    }
    
    # 注意：v3/detail 需要 property_id 参数
    $propertyId = "9884614204"
    $url = "https://$apiHost/properties/v3/detail?property_id=$propertyId"
    
    Write-Host "请求URL: $url" -ForegroundColor Gray
    
    $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers -TimeoutSec 20
    
    Write-Host "✓ API 连接成功!" -ForegroundColor Green
    Write-Host "响应数据:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3 | Write-Host
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "✗ 测试失败 (状态码: $statusCode)" -ForegroundColor Red
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "响应内容: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "---------------------------------" -ForegroundColor Gray
Write-Host ""

# 测试2: POST /properties/v3/list
Write-Host "【测试2】 POST /properties/v3/list" -ForegroundColor Yellow
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
    
    Write-Host "请求URL: $url" -ForegroundColor Gray
    Write-Host "请求体: $body" -ForegroundColor Gray
    
    $response = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -TimeoutSec 20
    
    Write-Host "✓ API 连接成功!" -ForegroundColor Green
    
    if ($response.data.home_search) {
        $total = $response.data.home_search.total
        $count = $response.data.home_search.count
        Write-Host "找到 $total 个房产，返回 $count 个" -ForegroundColor Green
        
        if ($response.data.home_search.results) {
            Write-Host ""
            Write-Host "前3个房产:" -ForegroundColor Green
            $index = 1
            foreach ($property in $response.data.home_search.results | Select-Object -First 3) {
                $propertyId = $property.property_id
                $address = $property.location.address.line
                $price = if ($property.list_price) { "$" + $property.list_price } else { "N/A" }
                
                Write-Host "  $index. ID: $propertyId, 地址: $address, 价格: $price" -ForegroundColor White
                $index++
            }
        }
    } else {
        Write-Host "响应数据:" -ForegroundColor Green
        $response | ConvertTo-Json -Depth 3 | Write-Host
    }
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "✗ 测试失败 (状态码: $statusCode)" -ForegroundColor Red
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "响应内容: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "---------------------------------" -ForegroundColor Gray
Write-Host ""

# 测试3: GET /finance/rates
Write-Host "【测试3】 GET /finance/rates" -ForegroundColor Yellow
try {
    $headers = @{
        "x-rapidapi-key" = $apiKey
        "x-rapidapi-host" = $apiHost
    }
    
    $url = "https://$apiHost/finance/rates"
    
    Write-Host "请求URL: $url" -ForegroundColor Gray
    
    $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers -TimeoutSec 20
    
    Write-Host "✓ API 连接成功!" -ForegroundColor Green
    Write-Host "响应数据:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3 | Write-Host
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "✗ 测试失败 (状态码: $statusCode)" -ForegroundColor Red
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "响应内容: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "---------------------------------" -ForegroundColor Gray
Write-Host ""

# 测试4: GET /mortgage/v2/check-rates
Write-Host "【测试4】 GET /mortgage/v2/check-rates" -ForegroundColor Yellow
try {
    $headers = @{
        "x-rapidapi-key" = $apiKey
        "x-rapidapi-host" = $apiHost
    }
    
    $url = "https://$apiHost/mortgage/v2/check-rates?postal_code=10001"
    
    Write-Host "请求URL: $url" -ForegroundColor Gray
    
    $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers -TimeoutSec 20
    
    Write-Host "✓ API 连接成功!" -ForegroundColor Green
    Write-Host "响应数据:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3 | Write-Host
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "✗ 测试失败 (状态码: $statusCode)" -ForegroundColor Red
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "响应内容: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=================================" -ForegroundColor Cyan
Write-Host "所有测试完成" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

