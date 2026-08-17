# ======================================================================
#  AlgoViz 非核心组件一键启动 (PowerShell 版，不会闪退)
#  启动顺序: Elasticsearch(9200) -> Kibana(5601) -> Fluentd -> Chroma(8000) -> Python(8001)
#  用法: 右键 -> 使用 PowerShell 运行；或 cmd 下 powershell -File "xxx.ps1"
# ======================================================================

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# ---- 路径常量 ----
$ES_BIN          = "D:\software\Elasticsearch\elasticsearch-7.12.1-windows-x86_64\elasticsearch-7.12.1\bin"
$KIBANA_BIN      = "D:\software\Kibana\kibana-7.12.1-windows-x86_64\kibana-7.12.1-windows-x86_64\bin"
$KNOW_DIR        = "D:\daima\XiangMu\算法数据结构可视化\AlgoVize\Agent\know-retrieval"
$ES_STARTUP      = Join-Path $ES_BIN     "elasticsearch.bat"
$KIBANA_STARTUP  = Join-Path $KIBANA_BIN "kibana.bat"
$FLUENT_CONF     = Join-Path $KNOW_DIR   "fluentd.conf"
$PYTHON_RUN      = Join-Path $KNOW_DIR   "run.py"
$CHROMA_DATA_DIR = Join-Path $KNOW_DIR   "chroma_data"

# ---- 工具函数 ----
function Write-Color($msg, $color = "Cyan") { Write-Host $msg -ForegroundColor $color }
function Write-OK   ($msg) { Write-Host "      [OK]  $msg" -ForegroundColor Green }
function Write-Warn ($msg) { Write-Host "      [!]   $msg" -ForegroundColor Yellow }
function Write-Fail ($msg) { Write-Host "      [X]   $msg" -ForegroundColor Red }
function Write-Step ($n, $t, $total = 5) { Write-Host "`n[$n/$total] $t..." -ForegroundColor White }

function Test-PortListening($port) {
    # 用 .NET 检测端口（比 netstat 快且准）
    try {
        $props = [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties()
        $listeners = $props.GetActiveTcpListeners()
        return [bool]($listeners | Where-Object { $_.Port -eq $port })
    } catch {
        # 回退到 netstat
        $r = netstat -ano 2>$null | Select-String -Pattern ":$port\s+.*LISTENING"
        return [bool]$r
    }
}

function Invoke-HealthCheck($url, $matchKeyword, $timeoutSec = 5) {
    try {
        $resp = Invoke-WebRequest -Uri $url -TimeoutSec $timeoutSec -UseBasicParsing -ErrorAction Stop
        if ([string]::IsNullOrWhiteSpace($matchKeyword)) { return $resp.StatusCode -ge 200 -and $resp.StatusCode -lt 400 }
        return ($resp.Content -match $matchKeyword)
    } catch {
        return $false
    }
}

function Wait-Condition([scriptblock]$cond, [int]$maxSeconds, [int]$intervalMs = 2000, [string]$desc = "就绪") {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $maxSeconds) {
        if (& $cond) { return $true }
        Start-Sleep -Milliseconds $intervalMs
    }
    return $false
}

function Start-NewWindow([string]$title, [string]$workDir, [string]$cmdExe, [string[]]$arguments) {
    # 用 cmd /k + START 拉起独立的最大化窗口，窗口关闭 = 服务停止
    $argsQuoted = ($arguments | ForEach-Object {
        if ($_ -match '\s') { "`"$_`"" } else { $_ }
    }) -join " "

    $psi = [Diagnostics.ProcessStartInfo]::new()
    $psi.FileName        = "cmd.exe"
    $psi.WorkingDirectory = if (Test-Path $workDir) { $workDir } else { (Get-Location).Path }
    $psi.Arguments       = "/k title $title && cd /d `"$workDir`" && $cmdExe $argsQuoted"
    $psi.WindowStyle     = [Diagnostics.ProcessWindowStyle]::Normal
    return [Diagnostics.Process]::Start($psi)
}

function Show-FailAndExit($step, $detail, $solution = "") {
    Write-Fail "$step 失败：$detail"
    if ($solution) { Write-Host "      解决建议：$solution" -ForegroundColor Yellow }

    # 弹窗 + 强制 pause（防止一闪而过）
    try {
        $null = (New-Object -ComObject WScript.Shell).PopUp(
            "[非核心组件启动失败]`n阶段: $step`n原因: $detail`n建议: $solution", 0, "AlgoViz 启动错误", 0x10)
    } catch {}

    Write-Host "`n==========================================================" -ForegroundColor Red
    Write-Host "  启动失败！请按上面提示修复后重试" -ForegroundColor Red
    Write-Host "  已启动的服务窗口请手动关闭" -ForegroundColor Yellow
    Write-Host "  端口释放命令: netstat -ano | Select-String ':9200|:5601|:8000|:8001'" -ForegroundColor Yellow
    Write-Host "  终止进程:   taskkill /F /PID 进程号" -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Red
    Read-Host "按回车键退出"
    exit 1
}

# ============================================================
# 开场
# ============================================================
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   AlgoViz 非核心组件一键启动 (PowerShell 版)" -ForegroundColor Cyan
Write-Host "   Elasticsearch + Kibana + Fluentd + Chroma + Python" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  启动顺序:"
Write-Host "    1) Elasticsearch  :9200"
Write-Host "    2) Kibana         :5601"
Write-Host "    3) Fluentd        :采集 D:\rizi -> ES"
Write-Host "    4) Chroma         :8000"
Write-Host "    5) Python 向量服务 :8001"
Write-Host "  每一步失败都会弹出错误框并停留，不会闪退。"
Write-Host "=========================================================="

# ============================================================
# STEP 1 - Elasticsearch
# ============================================================
Write-Step 1 "启动 Elasticsearch (端口 9200)"

if (-not (Test-Path $ES_STARTUP)) {
    Show-FailAndExit "Elasticsearch" "找不到 $ES_STARTUP" "检查 ES 安装路径是否被移动"
}
if (Test-PortListening 9200) {
    Write-Warn "端口 9200 已被占用，跳过启动"
} else {
    Start-NewWindow "Elasticsearch 7.12.1" $ES_BIN "elasticsearch.bat" | Out-Null
    $ok = Wait-Condition -maxSeconds 60 -desc "ES 启动" -cond {
        Invoke-HealthCheck "http://localhost:9200" "cluster_name" 3
    }
    if (-not $ok) {
        Show-FailAndExit "Elasticsearch" "60 秒内 http://localhost:9200 未响应" `
            "1) 查看 Elasticsearch 窗口日志 2) JVM 堆内存是否不足 3) data 目录是否被锁"
    }
    Write-OK "Elasticsearch 已就绪 (http://localhost:9200)"
}

# ============================================================
# STEP 2 - Kibana
# ============================================================
Write-Step 2 "启动 Kibana (端口 5601)"

if (-not (Test-Path $KIBANA_STARTUP)) {
    Show-FailAndExit "Kibana" "找不到 $KIBANA_STARTUP" "检查 Kibana 安装路径"
}
if (Test-PortListening 5601) {
    Write-Warn "端口 5601 已被占用，跳过启动"
} else {
    Start-NewWindow "Kibana 7.12.1" $KIBANA_BIN "kibana.bat" | Out-Null
    $ok = Wait-Condition -maxSeconds 120 -desc "Kibana 启动" -cond {
        Invoke-HealthCheck "http://localhost:5601/api/status" "green" 4
    }
    if (-not $ok) {
        Show-FailAndExit "Kibana" "120 秒内 /api/status 未返回 state=green" `
            "1) 确认 ES 已先启动 2) 查看 Kibana 窗口日志 3) 检查 kibana.yml 配置"
    }
    Write-OK "Kibana 已就绪 (http://localhost:5601)"
}

# ============================================================
# STEP 3 - Fluentd
# ============================================================
Write-Step 3 "启动 Fluentd (采集日志到 ES)"

if (-not (Test-Path $FLUENT_CONF)) {
    Show-FailAndExit "Fluentd" "找不到 $FLUENT_CONF"
}
# 检测 fluentd / td-agent 命令
$fluentCmd = $null
foreach ($c in @("fluentd", "td-agent")) {
    if (Get-Command $c -ErrorAction SilentlyContinue) { $fluentCmd = $c; break }
}
if (-not $fluentCmd) {
    Show-FailAndExit "Fluentd" "PATH 中找不到 fluentd 或 td-agent 命令" `
        "安装 Fluentd/TD-Agent，把其 bin 目录(如 D:\software\Fluentd\td-agent\bin)加入 PATH"
}
Write-Host "      使用命令: $fluentCmd" -ForegroundColor Gray

Start-NewWindow "Fluentd (日志采集器)" $KNOW_DIR $fluentCmd @("-c", "fluentd.conf") | Out-Null
Write-Host "      等待 15 秒启动..." -ForegroundColor Gray
Start-Sleep -Seconds 15

if (Test-PortListening 24220 -and (Invoke-HealthCheck "http://localhost:24220/api/plugins.json" "type" 3)) {
    Write-OK "Fluentd 已就绪 (监控端口 24220)"
} else {
    Write-Warn "Fluentd 监控口 24220 未响应（不代表采集失败）"
    Write-Host "      请打开 [Fluentd (日志采集器)] 窗口确认输出" -ForegroundColor Yellow
    Write-Host "      正常应看到 following tail of D:/rizi/info.log 等字样" -ForegroundColor Yellow
}

# ============================================================
# STEP 4 - Chroma 向量数据库
# ============================================================
Write-Step 4 "启动 Chroma 向量数据库 (端口 8000)"

if (-not (Get-Command "chroma" -ErrorAction SilentlyContinue)) {
    Show-FailAndExit "Chroma" "PATH 中找不到 chroma 命令" `
        "在 Python 环境中执行 pip install chromadb，并确认 Python\Scripts 加入 PATH"
}
if (Test-PortListening 8000) {
    Write-Warn "端口 8000 已被占用，跳过启动"
} else {
    Start-NewWindow "Chroma (向量数据库)" $KNOW_DIR "chroma" @(
        "run", "--path", "./chroma_data", "--host", "0.0.0.0", "--port", "8000"
    ) | Out-Null
    $ok = Wait-Condition -maxSeconds 30 -desc "Chroma 启动" -cond {
        # 新版 Chroma 心跳端点格式变化 -> 监听端口优先作为就绪判断
        (Test-PortListening 8000) -or (Invoke-HealthCheck "http://localhost:8000/api/v1/heartbeat" "heartbeat" 2)
    }
    if (-not $ok) {
        Show-FailAndExit "Chroma" "30 秒内端口 8000 未进入监听" `
            "1) 查看 Chroma 窗口日志 2) chroma_data 目录是否有写入权限 3) chromadb 版本兼容性"
    }
    Write-OK "Chroma 已就绪 (http://localhost:8000)"
}

# ============================================================
# STEP 5 - Python 向量服务
# ============================================================
Write-Step 5 "启动 Python 向量服务 FastAPI (端口 8001)"

if (-not (Test-Path $PYTHON_RUN)) {
    Show-FailAndExit "Python 服务" "找不到 $PYTHON_RUN"
}
if (-not (Get-Command "python" -ErrorAction SilentlyContinue)) {
    Show-FailAndExit "Python 服务" "PATH 中找不到 python 命令" "检查 Python 是否已安装并加 PATH"
}
if (Test-PortListening 8001) {
    Write-Warn "端口 8001 已被占用，跳过启动"
} else {
    Start-NewWindow "Python 向量服务 (FastAPI)" $KNOW_DIR "python" @("run.py") | Out-Null
    $ok = Wait-Condition -maxSeconds 60 -desc "Python 加载嵌入模型..." -cond {
        Invoke-HealthCheck "http://localhost:8001/health" "healthy|ok" 3
    }
    if (-not $ok) {
        Show-FailAndExit "Python 向量服务" "60 秒内 /health 未响应" `
            "1) sentence-transformers 模型是否已下载本地 2) 是否能连上 Chroma 8000 3) 查看 Python 窗口报错"
    }
    Write-OK "Python 向量服务已就绪 (http://localhost:8001)"
}

# ============================================================
# 全部成功
# ============================================================
Write-Host ""
Write-Host "========================================================" -ForegroundColor Green
Write-Host "   全部非核心组件启动成功！" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  服务访问地址:"
Write-Host "    Elasticsearch : http://localhost:9200"
Write-Host "    Kibana        : http://localhost:5601"
Write-Host "    Chroma        : http://localhost:8000"
Write-Host "    Python 服务   : http://localhost:8001"
Write-Host "    Fluentd 监控  : http://localhost:24220/api/plugins.json"
Write-Host ""
Write-Host "  日志存放目录    : D:\rizi"
Write-Host "  Kibana 索引模式 : algoviz-logs-*"
Write-Host "  Kibana 时间字段 : @timestamp"
Write-Host ""
Write-Host "  各服务在自己的窗口运行，关闭对应窗口即停止该服务。"
Write-Host "  本窗口可以直接关闭。" -ForegroundColor Yellow
Write-Host "========================================================"
Read-Host "按回车键关闭主控窗口（或直接点 X）"
exit 0
