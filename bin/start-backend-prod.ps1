<#
.SYNOPSIS
    AlgoViz 后端 jar 生产启动脚本（含 GC 日志 + JVM 崩溃日志 hs_err）
.DESCRIPTION
    用指定/自动识别的 JDK 以生产方式启动 Spring Boot jar，并开启：
      1) GC 日志   -> D:\rizi\JVM\jvm-gc-<启动时刻>-<进程ID>.log
                     （JDK9+ Unified Logging，filecount/filesize 滚动：
                       写满 10MB 自动滚动，归档后缀 .0~.9，.0 最新、数字越大越老，
                       基名含启动时刻+PID，重启后生成新文件）
      2) 崩溃日志   -> -XX:ErrorFile=D:\rizi\JVM\hs_err_pid<PID>.log
                     （JVM native crash / 段错误时自动生成）

    jar 来源（优先级）：
      1. 参数 -JarPath 指定（绝对路径，或相对本脚本目录的相对路径）
      2. 默认：自动取本脚本同级目录下唯一的 *.jar；
         有多个时取最新修改的并提示（可用 -JarPath 精确指定）

    JDK 识别（优先级从高到低）：
      1. -JavaHome 参数直接指定 JDK 根目录
      2. 环境变量 JAVA_HOME
      3. -JdkName 参数（如 "jdk17"，在 $JdkSearchRoots 下查找同名目录）
      4. PATH 中的 java
      5. 在 $JdkSearchRoots 下自动扫描 bin\java.exe

.EXAMPLE
    # 生产：jar 放在本脚本同级目录，自动识别 JDK，前台运行
    .\start-backend-prod.ps1

.EXAMPLE
    # 指定 jar 与 JDK，追加 JVM 参数，后台独立窗口运行
    .\start-backend-prod.ps1 -JarPath "D:\deploy\backend-1.0.0.jar" -JdkName "jdk17" -Xmx "2g" -Detach
.NOTES
    编码必须为 UTF-8 with BOM：Windows PowerShell 5.1 对无 BOM 的 .ps1 按 GBK 解析，
    中文注释会导致字节错乱报"字符串缺少终止符"。
    依赖：目标 JDK 必须是 9+（GC 用 Unified Logging -Xlog；低于 9 需改用 -XX:+PrintGCDetails 等）。
#>

[CmdletBinding()]
param(
    # jar 路径：绝对路径或相对本脚本目录；留空 = 自动找同级 *.jar
    [string]$JarPath = "",

    # 直接指定 JDK 根目录（最高优先级，会忽略 JdkName/JAVA_HOME）
    [string]$JavaHome = "",

    # JDK 目录名（如 "jdk17"），在 $JdkSearchRoots 下查找
    [string]$JdkName = "",

    # JDK 可能安装的父目录（会自动拼接 jdk 目录扫描）
    [string[]]$JdkSearchRoots = @("D:\software\jdk", "C:\Program Files\Java", "D:\software\java"),

    # 堆内存
    [string]$Xms = "256m",
    [string]$Xmx = "1g",

    # JVM 日志目录（GC 与 hs_err 都放这里）
    [string]$JvmLogDir = "D:\rizi\JVM",

    # GC 滚动：每个归档 10MB，保留 10 个（含当前），共约 100MB
    [int]$GcFileCount = 10,
    [string]$GcFileSize = "10m",

    # 追加的自定义 JVM 参数（会原样拼入 java 命令）
    [string]$ExtraJvmArgs = "",

    # 追加的应用参数（Spring Boot args，如 --server.port=xxxx）
    [string]$ExtraAppArgs = "",

    # 是否在独立新窗口后台运行（默认前台运行，Ctrl+C 停止）
    [switch]$Detach
)

$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

function Write-OK   ($m) { Write-Host "  [OK]   $m" -ForegroundColor Green }
function Write-Warn ($m) { Write-Host "  [!]    $m" -ForegroundColor Yellow }
function Write-Fail ($m) { Write-Host "  [X]    $m" -ForegroundColor Red }

# ============================================================
# 1. 定位 jar
# ============================================================
Write-Host "== AlgoViz 后端 jar 生产启动 ==" -ForegroundColor Cyan

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $jarCandidates = @(Get-ChildItem -Path $PSScriptRoot -Filter "*.jar" -File -ErrorAction SilentlyContinue)
    if ($jarCandidates.Count -eq 0) {
        Write-Fail "未找到 jar：$PSScriptRoot 下没有 *.jar"
        Write-Host "  请把 backend-1.0.0.jar 复制到本脚本目录，或 -JarPath 指定路径" -ForegroundColor Yellow
        Read-Host "按回车退出"; exit 1
    }
    if ($jarCandidates.Count -gt 1) {
        # 多个 jar：取最新修改的，并给出全部候选提示
        $jar = $jarCandidates | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        Write-Warn "同级目录发现 $($jarCandidates.Count) 个 jar，默认取最新："
        $jarCandidates | ForEach-Object { Write-Host "      $($_.Name)  $($_.LastWriteTime)" -ForegroundColor Gray }
    } else {
        $jar = $jarCandidates[0]
    }
    $JarPath = $jar.FullName
} else {
    if (-not [System.IO.Path]::IsPathRooted($JarPath)) { $JarPath = Join-Path $PSScriptRoot $JarPath }
    if (-not (Test-Path -LiteralPath $JarPath)) {
        Write-Fail "指定的 jar 不存在：$JarPath"; Read-Host "按回车退出"; exit 1
    }
}
Write-OK "jar = $JarPath"

# ============================================================
# 2. 定位 JDK
# ============================================================
$javaExe = $null

function Test-JavaExe([string]$java) {
    if (-not [string]::IsNullOrWhiteSpace($java) -and (Test-Path -LiteralPath $java)) { return $true }
    return $false
}

# 2.1 参数 -JavaHome
if (Test-JavaExe (Join-Path $JavaHome "bin\java.exe")) {
    $javaExe = Join-Path $JavaHome "bin\java.exe"
}
# 2.2 环境变量 JAVA_HOME
if (-not $javaExe -and $env:JAVA_HOME) {
    if (Test-JavaExe (Join-Path $env:JAVA_HOME "bin\java.exe")) {
        $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
    } else {
        Write-Warn "JAVA_HOME 指向的目录无 java.exe：$env:JAVA_HOME"
    }
}
# 2.3 参数 -JdkName：在搜索根下找同名目录
if (-not $javaExe -and $JdkName) {
    foreach ($root in $JdkSearchRoots) {
        $cand = Join-Path (Join-Path $root $JdkName) "bin\java.exe"
        if (Test-JavaExe $cand) { $javaExe = $cand; break }
    }
    if (-not $javaExe) { Write-Warn "在 $($JdkSearchRoots -join ', ') 下找不到目录名 $JdkName" }
}
# 2.4 PATH 中的 java
if (-not $javaExe) {
    $cmd = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($cmd) {
        # 解析为真实路径（cmd 可能是 shim）
        try { $javaExe = $cmd.Source } catch { $javaExe = $cmd.Path }
    }
}
# 2.5 扫描搜索根，选版本最高的 java
if (-not $javaExe) {
    $found = @()
    foreach ($root in $JdkSearchRoots) {
        if (Test-Path -LiteralPath $root) {
            Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue | ForEach-Object {
                $j = Join-Path $_.FullName "bin\java.exe"
                if (Test-JavaExe $j) { $found += [pscustomobject]@{ Path = $j; Dir = $_.Name } }
            }
        }
    }
    if ($found.Count -eq 0) {
        Write-Fail "未能自动识别任何 JDK。请用 -JavaHome 指定，或 -JdkName 指定目录名"
        Read-Host "按回车退出"; exit 1
    }
    # 按名称中的数字排序（如 jdk17 / jdk-21），取最大的
    $javaExe = ($found | Sort-Object { [regex]::Replace($_.Dir, '\D', '') -as [int] } -Descending | Select-Object -First 1).Path
}
Write-OK "java = $javaExe"

# ============================================================
# 3. 准备 JVM 日志目录
# ============================================================
if (-not (Test-Path -LiteralPath $JvmLogDir)) {
    New-Item -Path $JvmLogDir -ItemType Directory -Force | Out-Null
}

# ============================================================
# 4. 组装 JVM 参数
# ============================================================
# GC：Unified Logging。文件名可用 %t(启动时刻) %p(进程ID) 两个占位符。
#    例：jvm-gc-2026-09-05_22-10-30-12456.log；滚动归档在其后追加 .0~.9。
$gcLogFile = "$($JvmLogDir -replace '\\','/')/jvm-gc-%t-%p.log"
$gcArgs = @(
    "-Xms$Xms",
    "-Xmx$Xmx",
    "-Xlog:gc*:file=$gcLogFile:time,uptime,level,tags:filecount=$GcFileCount,filesize=$GcFileSize",
    "-XX:ErrorFile=$($JvmLogDir -replace '\\','/')/hs_err_pid%p.log"
)

# 追加自定义 JVM 参数（按空格拆分，支持 "key=value" 无需引号）
if (-not [string]::IsNullOrWhiteSpace($ExtraJvmArgs)) {
    $gcArgs += ($ExtraJvmArgs -split '\s+' | Where-Object { $_ })
}

# 应用参数（Spring Boot）
$appArgs = @()
if (-not [string]::IsNullOrWhiteSpace($ExtraAppArgs)) {
    $appArgs += ($ExtraAppArgs -split '\s+' | Where-Object { $_ })
}

Write-Host ""
Write-Host "  GC 日志    : $($JvmLogDir)\jvm-gc-<启动时刻>-<PID>.log (归档 .0~$($GcFileCount-1))" -ForegroundColor Gray
Write-Host "  崩溃日志   : $($JvmLogDir)\hs_err_pid<PID>.log" -ForegroundColor Gray
Write-Host "  堆内存     : -Xms$Xms -Xmx$Xmx" -ForegroundColor Gray
Write-Host ""

# DEEPSEEK_API_KEY 提醒（后端 AI 功能依赖环境变量）
if ([string]::IsNullOrWhiteSpace($env:DEEPSEEK_API_KEY)) {
    Write-Warn "环境变量 DEEPSEEK_API_KEY 未设置，AI 对话/向量功能可能不可用"
}

# ============================================================
# 5. 启动
# ============================================================
$allArgs = $gcArgs + $appArgs + @($JarPath)

if ($Detach) {
    $psi = [Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $javaExe
    $psi.WorkingDirectory = Split-Path -Parent $JarPath
    $psi.Arguments = ($allArgs | ForEach-Object { if ($_ -match '\s') { "`"$_`"" } else { $_ } }) -join " "
    $psi.UseShellExecute = $true
    $proc = [Diagnostics.Process]::Start($psi)
    Write-OK "已在独立窗口后台启动，PID=$($proc.Id)"
    Write-Host "  关闭该窗口或结束进程即停止服务。" -ForegroundColor Yellow
} else {
    Write-Host "== 正在前台启动（Ctrl+C 停止）..." -ForegroundColor Green
    & $javaExe @allArgs
    $code = $LASTEXITCODE
    Write-Host ""
    Write-Host "== 进程已退出，退出码 = $code ==" -ForegroundColor Yellow
    exit $code
}
