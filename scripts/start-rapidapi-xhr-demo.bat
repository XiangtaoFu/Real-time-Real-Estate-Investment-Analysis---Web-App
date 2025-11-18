@echo off
setlocal
REM Open the RapidAPI XHR demo HTML in the default browser
set ROOT=%~dp0..
set DEMO=%ROOT%\docs\demos\rapidapi-xhr-demo.html
if not exist "%DEMO%" (
  echo Could not find %DEMO%
  exit /b 1
)
start "" "%DEMO%"
endlocal
