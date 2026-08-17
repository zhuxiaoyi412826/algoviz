@echo off
chcp 65001 >nul 2>nul
title AlgoViz 一键关闭
color 0C

echo ========================================
echo    AlgoViz 一键关闭脚本
echo ========================================
echo.

set "CLOSED_COUNT=0"

:: ========== 1. 关闭后端 (端口 80) ==========
echo [1/3] 关闭后端服务 (端口 80)...
set "FOUND_BACKEND=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":80 " ^| findstr "LISTENING"') do (
    taskkill /PID %%a /F >nul 2>nul
    set "FOUND_BACKEND=1"
)
:: 关闭后端命令行窗口
taskkill /FI "WINDOWTITLE eq AlgoViz-Backend*" /F >nul 2>nul
if "%FOUND_BACKEND%"=="1" (
    echo       [成功] 后端服务已关闭
    set /a CLOSED_COUNT+=1
) else (
    echo       [跳过] 后端服务未在运行
)

timeout /t 1 /nobreak >nul

:: ========== 2. 关闭后台管理 (端口 5000) ==========
echo [2/3] 关闭后台管理系统 (端口 5000)...
set "FOUND_ADMIN=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5000 " ^| findstr "LISTENING"') do (
    taskkill /PID %%a /F >nul 2>nul
    set "FOUND_ADMIN=1"
)
:: 关闭后台管理命令行窗口
taskkill /FI "WINDOWTITLE eq AlgoViz-Admin*" /F >nul 2>nul
if "%FOUND_ADMIN%"=="1" (
    echo       [成功] 后台管理系统已关闭
    set /a CLOSED_COUNT+=1
) else (
    echo       [跳过] 后台管理系统未在运行
)

timeout /t 1 /nobreak >nul

:: ========== 3. 关闭前端 (端口 5500) ==========
echo [3/3] 关闭前端服务 (端口 5500)...
set "FOUND_FRONTEND=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5500 " ^| findstr "LISTENING"') do (
    taskkill /PID %%a /F >nul 2>nul
    set "FOUND_FRONTEND=1"
)
:: 关闭前端命令行窗口
taskkill /FI "WINDOWTITLE eq AlgoViz-Frontend*" /F >nul 2>nul
if "%FOUND_FRONTEND%"=="1" (
    echo       [成功] 前端服务已关闭
    set /a CLOSED_COUNT+=1
) else (
    echo       [跳过] 前端服务未在运行
)

echo.
echo [清理] 清理残留的命令行窗口...
:: 清理所有 AlgoViz- 开头的命令行窗口
taskkill /FI "WINDOWTITLE eq AlgoViz-*" /F >nul 2>nul

echo.
echo ========================================
echo    所有服务已关闭！
echo ----------------------------------------
echo    共关闭 %CLOSED_COUNT% 个服务
echo    命令行窗口已清理
echo ========================================
echo.
pause
exit /b 0
