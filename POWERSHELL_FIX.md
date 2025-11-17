# PowerShell PSReadLine 问题解决方案

## 问题描述
在当前的 PowerShell 终端中出现 PSReadLine 模块错误：
```
System.ArgumentOutOfRangeException: 该值必须大于或等于零，且必须小于控制台缓冲区在该维度的大小。
参数名: top
```

这是因为终端输出过多导致 PSReadLine 无法正确处理光标位置。

## 解决方案

### 方案 1: 使用新的 PowerShell 窗口运行命令

启动服务：
```powershell
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\14215\Desktop\Real-Time-Real-Estate-Investment-Analysis--Web-App\Real-Time-Real-Estate-Investment-Analysis--Web-App\realtyinus'; `$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot'; & 'C:\Program Files\apache-maven-3.9.11\bin\mvn.cmd' spring-boot:run"
```

运行测试：
```powershell
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", "test-api.ps1"
```

### 方案 2: 禁用 PSReadLine（临时）

```powershell
Remove-Module PSReadLine
```

然后运行您的命令。这会在当前会话中禁用 PSReadLine。

### 方案 3: 清理终端后再运行

```powershell
Clear-Host
# 然后运行您的命令
```

### 方案 4: 使用简化的命令

避免复杂的管道和格式化命令：

```powershell
# 简单测试
Invoke-WebRequest -Uri "http://localhost:8081/api/properties/3325825129/investment-data" -Method GET -UseBasicParsing

# 保存到文件而不是在终端显示
Invoke-RestMethod -Uri "http://localhost:8081/api/properties/3325825129/investment-data" | ConvertTo-Json -Depth 10 | Out-File result.json
```

## 推荐的测试流程

### 步骤 1: 启动服务（在新窗口）
运行已创建的脚本：
```powershell
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\14215\Desktop\Real-Time-Real-Estate-Investment-Analysis--Web-App\Real-Time-Real-Estate-Investment-Analysis--Web-App\realtyinus'; `$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot'; & 'C:\Program Files\apache-maven-3.9.11\bin\mvn.cmd' spring-boot:run"
```

### 步骤 2: 等待 15-20 秒让服务完全启动

### 步骤 3: 运行测试（在新窗口）
```powershell
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", "test-api.ps1"
```

或者直接在文件浏览器中右键点击 `test-api.ps1` → 选择 "使用 PowerShell 运行"

## 已创建的测试脚本

### test-api.ps1
位置: `c:\Users\14215\Desktop\Real-Time-Real-Estate-Investment-Analysis--Web-App\Real-Time-Real-Estate-Investment-Analysis--Web-App\test-api.ps1`

功能：
- 等待服务启动
- 测试 Market Data API
- 显示实时获取的数据：
  - Mortgage Rate (房贷利率)
  - Estimated Rent (估计租金)
  - Property Tax Rate (房产税率)
  - Average HOA (平均HOA费用)
- 将完整响应保存到 `market-data-result.json`

## 检查服务状态

简单检查服务是否运行：
```powershell
Test-NetConnection -ComputerName localhost -Port 8081
```

或者：
```powershell
netstat -ano | findstr :8081
```

## 服务端口

- realtyinus API: **8081**
- googlemapv2 API: **8080**

## 快速命令

停止占用端口的进程：
```powershell
# 查找进程ID
netstat -ano | findstr :8081

# 停止进程（替换 <PID> 为实际进程ID）
taskkill /F /PID <PID>
```

重新启动服务：
```powershell
cd realtyinus
& "C:\Program Files\apache-maven-3.9.11\bin\mvn.cmd" spring-boot:run
```
