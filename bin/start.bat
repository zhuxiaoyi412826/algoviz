@echo off
chcp 65001 >nul 2>nul
title AlgoViz 一键启动
color 0A

echo ========================================
echo    AlgoViz 一键启动脚本
echo ========================================
echo.

set "ROOT=%~dp0.."
set "FRONTEND=%ROOT%\qianduan"
set "ADMIN=%ROOT%\houtai"
set "BACKEND=%ROOT%\houduan"

:: ========== 1. 启动前端 ==========
echo [1/3] 启动前端服务 (端口 5500)...

if not exist "%FRONTEND%\index.html" (
    echo [失败] 找不到前端入口文件: %FRONTEND%\index.html
    msg * "前端启动失败：找不到 qianduan\index.html，请检查前端目录"
    goto :FAILED
)

:: 检查端口是否被占用
netstat -ano | findstr ":5500 " | findstr "LISTENING" >nul 2>nul
if %errorlevel% equ 0 (
    echo [失败] 端口 5500 已被占用
    msg * "前端启动失败：端口 5500 已被占用，请先运行 stop.bat 关闭旧服务"
    goto :FAILED
)

:: 优先用 Python 启动，其次用 Node.js
where python >nul 2>nul
if %errorlevel% equ 0 (
    start "AlgoViz-Frontend" cmd /k "chcp 65001 >nul & title AlgoViz-Frontend(5500) & cd /d "%FRONTEND%" & echo 前端服务运行中 http://localhost:5500 & python -m http.server 5500"
    echo       [成功] 前端已启动 -^> http://localhost:5500
) else (
    where npx >nul 2>nul
    if %errorlevel% equ 0 (
        start "AlgoViz-Frontend" cmd /k "chcp 65001 >nul & title AlgoViz-Frontend(5500) & cd /d "%FRONTEND%" & echo 前端服务运行中 http://localhost:5500 & npx http-server -p 5500 -c-1"
        echo       [成功] 前端已启动 -^> http://localhost:5500
    ) else (
        echo [失败] 未找到 Python 或 Node.js，无法启动前端
        msg * "前端启动失败：未找到 Python 或 Node.js，请安装 Python 3 或 Node.js"
        goto :FAILED
    )
)

timeout /t 3 /nobreak >nul

:: ========== 2. 启动后台管理 ==========
echo [2/3] 启动后台管理系统 (端口 5000)...

if not exist "%ADMIN%\package.json" (
    echo [失败] 找不到后台管理配置: %ADMIN%\package.json
    msg * "后台管理启动失败：找不到 houtai\package.json，请检查目录"
    goto :FAILED
)

netstat -ano | findstr ":5000 " | findstr "LISTENING" >nul 2>nul
if %errorlevel% equ 0 (
    echo [失败] 端口 5000 已被占用
    msg * "后台管理启动失败：端口 5000 已被占用，请先运行 stop.bat 关闭旧服务"
    goto :FAILED
)

where npm >nul 2>nul
if %errorlevel% neq 0 (
    echo [失败] 未找到 npm，无法启动后台管理
    msg * "后台管理启动失败：未找到 npm，请安装 Node.js"
    goto :FAILED
)

:: 检查 node_modules 是否存在
if not exist "%ADMIN%\node_modules" (
    echo       首次运行，正在安装依赖（可能需要几分钟）...
    cd /d "%ADMIN%"
    call npm install
    if %errorlevel% neq 0 (
        echo [失败] 依赖安装失败
        msg * "后台管理启动失败：npm install 失败，请检查网络或手动运行 npm install"
        goto :FAILED
    )
)

start "AlgoViz-Admin" cmd /k "chcp 65001 >nul & title AlgoViz-Admin(5000) & cd /d "%ADMIN%" & echo 后台管理系统运行中 http://localhost:5000 & npm run dev"
echo       [成功] 后台管理已启动 -^> http://localhost:5000

timeout /t 3 /nobreak >nul

:: ========== 3. 启动后端 ==========
echo [3/3] 启动后端服务 (端口 80)...

if not exist "%BACKEND%\pom.xml" (
    echo [失败] 找不到后端配置: %BACKEND%\pom.xml
    msg * "后端启动失败：找不到 houduan\pom.xml，请检查目录"
    goto :FAILED
)

netstat -ano | findstr ":80 " | findstr "LISTENING" >nul 2>nul
if %errorlevel% equ 0 (
    echo [失败] 端口 80 已被占用
    msg * "后端启动失败：端口 80 已被占用，请先运行 stop.bat 关闭旧服务"
    goto :FAILED
)

:: 检查 Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [失败] 未找到 Java，请安装 JDK 17+
    msg * "后端启动失败：未找到 Java，请安装 JDK 17 或以上版本"
    goto :FAILED
)

:: 检查 Maven
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [失败] 未找到 Maven，请安装 Apache Maven
    msg * "后端启动失败：未找到 Maven，请安装 Apache Maven 并配置环境变量"
    goto :FAILED
)

:: 检查 MySQL 是否运行 (端口 3306)
netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul 2>nul
if %errorlevel% neq 0 (
    echo [警告] MySQL 未在端口 3306 运行，后端可能启动失败
    echo        请确保 MySQL 已启动且数据库 algoviz 已创建
    msg * "警告：MySQL 未检测到运行（端口3306），后端可能启动失败！请确保 MySQL 已启动"
)

:: 检查 DEEPSEEK_API_KEY 环境变量
if "%DEEPSEEK_API_KEY%"=="" (
    echo [警告] 未设置 DEEPSEEK_API_KEY 环境变量，AI 功能可能不可用
)

start "AlgoViz-Backend" cmd /k "chcp 65001 >nul & title AlgoViz-Backend(80) & cd /d "%BACKEND%" & echo 后端服务运行中 http://localhost:80 & mvn spring-boot:run"
echo       [成功] 后端已启动 -^> http://localhost:80

echo.
echo ========================================
echo    所有服务已启动！
echo ----------------------------------------
echo    前端地址:     http://localhost:5500
echo    后台管理:     http://localhost:5000
echo    后端API:      http://localhost:80
echo    Swagger文档:  http://localhost:80/swagger-ui.html
echo ----------------------------------------
echo    关闭所有服务请运行: stop.bat
echo ========================================
echo.
pause
exit /b 0

:FAILED
echo.
echo [错误] 启动过程中出现失败，已终止
echo.
pause
exit /b 1
