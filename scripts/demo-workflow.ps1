# Demo Workflow Script: Search by ZIP → pick property_id → show details & similar comps
# Usage: Right-click and Run with PowerShell, or run from PowerShell: .\scripts\demo-workflow.ps1

param(
    [string]$BaseUrl = "http://localhost:8081"
)

Write-Host ("=" * 70) -ForegroundColor Gray
Write-Host "RealtyInUS Demo: ZIP → List → Detail → Similar" -ForegroundColor Cyan
Write-Host ("=" * 70) -ForegroundColor Gray

# 0) Quick readiness probe
try {
    $null = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Method Head -Uri $BaseUrl
} catch {
    Write-Host "提示：服务可能尚未启动或端口不同。请先运行: .\\scripts\\start-realtyinus.bat" -ForegroundColor Yellow
    Write-Host "或将 -BaseUrl 参数改为正确端口，例如 http://localhost:8080" -ForegroundColor Yellow
}

# 1) Ask ZIP code
$zip = Read-Host "请输入 ZIP Code (例如 02171 或 02215)"
if (-not $zip) {
    Write-Host "未输入 ZIP，已取消。" -ForegroundColor Red
    exit 1
}

# 2) Search properties
$searchUrl = "$BaseUrl/api/properties/search?postalCode=$zip&status=for_sale&limit=5"
Write-Host "\n[1] 搜索房源: $searchUrl" -ForegroundColor Cyan

try {
    $search = Invoke-RestMethod -TimeoutSec 30 -Uri $searchUrl -Method GET
} catch {
    Write-Host "搜索请求失败：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

if (-not $search -or -not $search.properties -or $search.properties.Count -eq 0) {
    Write-Host "未找到房源，请尝试更换 ZIP 或稍后再试。" -ForegroundColor Yellow
    exit 0
}

Write-Host "\n返回前 5 条房源：" -ForegroundColor Green
$search.properties |
  Select-Object -First 5 @{N='propertyId';E={$_.propertyId}}, @{N='address';E={$_.address}}, city, state, @{N='listPrice';E={$_.listPrice}}, beds, baths, sqft |
  Format-Table -AutoSize

$defaultPropId = ($search.properties | Select-Object -First 1).propertyId
$propIdInput = Read-Host "\n请输入 property_id（直接回车使用默认 $defaultPropId）"
$propId = if ([string]::IsNullOrWhiteSpace($propIdInput)) { $defaultPropId } else { $propIdInput.Trim() }

if (-not $propId) {
    Write-Host "未选择 property_id，已取消。" -ForegroundColor Red
    exit 1
}

# 3) Fetch detail
$detailUrl = "$BaseUrl/api/properties/$propId"
Write-Host "\n[2] 获取详情: $detailUrl" -ForegroundColor Cyan
try {
    $detail = Invoke-RestMethod -TimeoutSec 30 -Uri $detailUrl -Method GET
} catch {
    Write-Host "详情请求失败：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "\n详情-关键信息：" -ForegroundColor Green
if ($detail -and $detail.propertyInfo) {
    $detail.propertyInfo | Select-Object propertyId, address, city, state, postalCode, listPrice, beds, baths, sqft, status | Format-List
} else {
    $detail | ConvertTo-Json -Depth 6 | Write-Output
}

# 4) Fetch similar comps
$similarUrl = "$BaseUrl/api/properties/$propId/similar?limit=5"
Write-Host "\n[3] 相似房源（comps）: $similarUrl" -ForegroundColor Cyan

try {
    $similar = Invoke-RestMethod -TimeoutSec 30 -Uri $similarUrl -Method GET
} catch {
    Write-Host "相似房源请求失败：$($_.Exception.Message)" -ForegroundColor Red
    $similar = $null
}

if ($similar) {
    $similar | Select-Object -First 5 @{N='propertyId';E={$_.propertyId}}, @{N='address';E={$_.address}}, @{N='price';E={$_.price}}, beds, baths, sqft, status |
      Format-Table -AutoSize
} else {
    Write-Host "未返回相似房源或接口暂不可用。" -ForegroundColor Yellow
}

# 5) Save artifacts for review
$searchPath  = Join-Path -Path (Get-Location) -ChildPath ("search_" + $zip + ".json")
$detailPath  = Join-Path -Path (Get-Location) -ChildPath ("detail_" + $propId + ".json")
$similarPath = Join-Path -Path (Get-Location) -ChildPath ("similar_" + $propId + ".json")

try { $search  | ConvertTo-Json -Depth 8 | Out-File -FilePath $searchPath  -Encoding UTF8 } catch {}
try { $detail  | ConvertTo-Json -Depth 8 | Out-File -FilePath $detailPath  -Encoding UTF8 } catch {}
try { if ($similar) { $similar | ConvertTo-Json -Depth 8 | Out-File -FilePath $similarPath -Encoding UTF8 } } catch {}

Write-Host "\n已保存结果文件：" -ForegroundColor Green
Write-Host " - $searchPath" -ForegroundColor Gray
Write-Host " - $detailPath" -ForegroundColor Gray
if ($similar) { Write-Host " - $similarPath" -ForegroundColor Gray }

Write-Host "\n演示完成。" -ForegroundColor Cyan
