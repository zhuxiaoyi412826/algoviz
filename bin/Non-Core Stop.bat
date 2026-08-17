@echo off
REM ============================================================
REM   AlgoViz Non-Core Components Stop (Double-click Entry)
REM   -> Calls PowerShell version: Non-Core Stop.ps1
REM   -> Stop order: Fluentd -> Kibana -> Elasticsearch
REM                  -> Python -> Chroma
REM   -> Encoding-safe / No flash-close / Errors hold with popup
REM ============================================================

title AlgoViz Non-Core Stop (Entry)

setlocal
set "SCRIPT_DIR=%~dp0"
set "PS1=%SCRIPT_DIR%Non-Core Stop.ps1"

REM ---- 1) Check ps1 exists ----
if not exist "%PS1%" (
    echo [ERROR] PowerShell stop script not found:
    echo        %PS1%
    echo.
    echo Please confirm Non-Core Stop.ps1 in bin directory is not deleted.
    pause
    exit /b 1
)

REM ---- 2) Check powershell ----
where powershell >nul 2>nul
if errorlevel 1 (
    echo [ERROR] PowerShell not detected on system.
    pause
    exit /b 1
)

echo Starting PowerShell stop script...
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "& '%PS1%'"

REM ---- 3) Catch abnormal exit ----
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
    echo.
    echo [ABNORMAL EXIT] PowerShell return code: %EXIT_CODE%
    echo.
    echo To see detailed error, run manually in PowerShell:
    echo   powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1%"
    echo.
    pause
    exit /b %EXIT_CODE%
)

endlocal
exit /b 0