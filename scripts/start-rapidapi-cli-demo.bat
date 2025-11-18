@echo off
setlocal
set ROOT=%~dp0..
set DEMO=%ROOT%\scripts\rapidapi-cli-demo.ps1
if not exist "%DEMO%" (
  echo Could not find %DEMO%
  exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%DEMO%" %*
endlocal
