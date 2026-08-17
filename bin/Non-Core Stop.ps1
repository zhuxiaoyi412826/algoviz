# ======================================================================
#  AlgoViz 非核心组件一键关闭 (PowerShell 版)
#  关闭顺序: 1.Fluentd -> 2.Kibana -> 3.Elasticsearch
#            -> 4.Python 向量服务 -> 5.Chroma 向量数据库
#  原理: 1. 通过端口/窗口标题找到服务进程 (java/node/python)
#        2. 向上递归查父进程链, 找到最顶层 cmd.exe (黑窗口)
#        3. taskkill /T /F 最顶层 cmd.exe -> 整棵进程树被连根拔起
#        4. 窗口消失 + 端口释放 + 服务停止, 一步到位
# ======================================================================

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# ---- 端口常量 ----
$PORT_FLUENTD   = 24220
$PORT_KIBANA    = 5601
$PORT_ES        = 9200
$PORT_PYTHON    = 8001
$PORT_CHROMA    = 8000

# ---- 窗口标题匹配关键字 ----
$WINDOW_FLUENTD = "Fluentd*"
$WINDOW_KIBANA  = "Kibana*"
$WINDOW_ES      = "Elasticsearch*"
$WINDOW_PYTHON  = "Python 向量服务*"
$WINDOW_CHROMA  = "Chroma*"

# ---- 工具函数 ----
function Write-Step($n, $t, $total = 5) { Write-Host "`n[$n/$total] $t..." -ForegroundColor White }
function Write-OK   ($msg) { Write-Host "      [OK]  $msg" -ForegroundColor Green }
function Write-Warn ($msg) { Write-Host "      [!]   $msg" -ForegroundColor Yellow }
function Write-Fail ($msg) { Write-Host "      [X]   $msg" -ForegroundColor Red }

function Test-PortListening($port) {
    try {
        $props = [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties()
        $listeners = $props.GetActiveTcpListeners()
        return [bool]($listeners | Where-Object { $_.Port -eq $port })
    } catch {
        $r = netstat -ano 2>$null | Select-String -Pattern ":$port\s+.*LISTENING"
        return [bool]$r
    }
}

function Get-ProcessByPort($port) {
    try {
        $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if ($conns) {
            $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
            $procs = foreach ($pid_val in $pids) {
                if ($pid_val -and $pid_val -ne 0) { Get-Process -Id $pid_val -ErrorAction SilentlyContinue }
            }
            return $procs | Where-Object { $_ }
        }
    } catch {
        $lines = netstat -ano 2>$null | Select-String -Pattern ":$port\s+.*LISTENING"
        $pids = foreach ($l in $lines) { if ($l -match '\s+(\d+)\s*$') { $matches[1] } }
        $procs = foreach ($pid_val in ($pids | Select-Object -Unique)) {
            if ($pid_val) { Get-Process -Id $pid_val -ErrorAction SilentlyContinue }
        }
        return $procs | Where-Object { $_ }
    }
}

function Get-ProcessByWindowTitle($pattern) {
    try {
        return Get-Process | Where-Object { $_.MainWindowTitle -like $pattern }
    } catch {
        return $null
    }
}

# ============================================================
# 关键函数: 向上递归查父进程链, 找到最顶层的 cmd.exe
# 这是黑窗口的来源, 杀掉它 + /T 就能连根拔起整棵树
# ============================================================
function Get-TopParentCmd([int]$procId) {
    $topCmd = $null
    $current = $procId
    $visited = @{}  # 防止循环引用

    while ($current -and $current -ne 0 -and $current -ne 4 -and -not $visited.ContainsKey($current)) {
        $visited[$current] = $true
        try {
            $wmiProc = Get-CimInstance Win32_Process -Filter "ProcessId=$current" -ErrorAction SilentlyContinue
            if (-not $wmiProc) { break }

            $p = Get-Process -Id $current -ErrorAction SilentlyContinue
            if ($p -and $p.ProcessName -eq "cmd") {
                # 记录最新发现的 cmd.exe (越往上越顶层)
                $topCmd = $current
            }

            # 继续向上查
            $current = [int]$wmiProc.ParentProcessId
        } catch {
            break
        }
    }

    return $topCmd
}

# ============================================================
# 关键函数: 终止进程树
# 策略: 找到最顶层 cmd.exe -> taskkill /T /F 一锅端
# ============================================================
function Stop-ProcessTree([int]$procId, [string]$name) {
    # 1. 找最顶层 cmd.exe 父进程
    $topCmd = Get-TopParentCmd $procId

    # 2. 确定要 taskkill 的目标 PID
    #    如果有顶层 cmd.exe, 杀它 (连带子进程全部退出, 包括黑窗口)
    #    如果没有 (进程不是从 cmd.exe 启动的), 直接杀进程本身
    $targetPid = if ($topCmd) { $topCmd } else { $procId }

    # 3. 收集要显示的进程信息
    $allPids = @()
    if ($topCmd) {
        # 列出从顶层 cmd 到目标进程的所有 PID (用 CIM 查整个树)
        $allPids += $topCmd
        # 也加入最初找到的进程
        $allPids += $procId
    } else {
        $allPids += $procId
    }
    $allPids = $allPids | Select-Object -Unique

    Write-Host "      终止进程树 (根 PID: $targetPid)" -ForegroundColor Gray
    foreach ($pid_val in $allPids) {
        try {
            $p = Get-Process -Id $pid_val -ErrorAction SilentlyContinue
            if ($p) {
                $mark = if ($pid_val -eq $targetPid) { " <- taskkill 目标" } else { "" }
                Write-Host "        -> PID $pid_val ($($p.ProcessName)$mark)" -ForegroundColor Gray
            }
        } catch {}
    }

    # 4. 执行 taskkill /T /F (T=Tree 连同子进程, F=Force 不弹确认)
    $output = & taskkill /PID $targetPid /T /F 2>&1
    if ($LASTEXITCODE -eq 0) {
        return $true
    } else {
        # taskkill 失败, 回退
        Write-Warn "taskkill 失败: $output"
        Write-Host "      尝试单独终止..." -ForegroundColor Gray
        # 先杀子进程
        try {
            $children = Get-CimInstance Win32_Process | Where-Object { $_.ParentProcessId -eq $targetPid }
            foreach ($child in $children) {
                try { Stop-Process -Id $child.ProcessId -Force -ErrorAction SilentlyContinue } catch {}
            }
        } catch {}
        # 再杀目标
        try { Stop-Process -Id $targetPid -Force -ErrorAction SilentlyContinue } catch {}
        # 也杀最初找到的进程
        if ($procId -ne $targetPid) {
            try { Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue } catch {}
        }
        return $false
    }
}

function Stop-Step([string]$stepName, [int]$port, [string]$windowPattern, [int]$waitSec = 2) {
    Write-Host ""
    Write-Host "------------------------------------------------------------"
    $procs = @()

    if ($port -gt 0) { $procs += (Get-ProcessByPort $port) }
    if ($windowPattern) { $procs += (Get-ProcessByWindowTitle $windowPattern) }

    # 去重
    $procs = $procs | Sort-Object -Property Id -Unique | Where-Object { $_ }

    if (-not $procs -or $procs.Count -eq 0) {
        Write-Warn "$stepName : 未发现运行中的进程 (可能已停止)"
        return
    }

    Write-Host "  发现 $($procs.Count) 个目标进程, 开始终止..." -ForegroundColor Gray
    foreach ($p in $procs) {
        Write-Host "    PID $($p.Id) | $($p.ProcessName) | 窗口: $($p.MainWindowTitle)" -ForegroundColor Gray
    }

    # 对每个进程: 找最顶层 cmd.exe 父进程, 然后 taskkill /T /F
    # 用 HashSet 去重, 避免对同一个 cmd.exe 重复 taskkill
    $killedTargets = @{}
    foreach ($p in $procs) {
        $topCmd = Get-TopParentCmd $p.Id
        $target = if ($topCmd) { $topCmd } else { $p.Id }
        if (-not $killedTargets.ContainsKey($target)) {
            $killedTargets[$target] = $true
            $null = Stop-ProcessTree -procId $p.Id -name $stepName
        }
    }

    # 等端口释放
    Start-Sleep -Seconds $waitSec

    # 验证: 检查所有原目标进程是否已退出
    $still = $procs | Where-Object { -not $_.HasExited }
    if ($still.Count -eq 0) {
        Write-OK "$stepName 已全部终止 (黑窗口已关闭)"
    } else {
        Write-Fail "$stepName 仍有 $($still.Count) 个进程未终止"
        foreach ($p in $still) {
            Write-Host "        PID $($p.Id) ($($p.ProcessName))" -ForegroundColor Red
        }
    }
}

# ============================================================
# 开场
# ============================================================
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   AlgoViz 非核心组件一键关闭 (PowerShell 版)" -ForegroundColor Cyan
Write-Host "   关闭顺序: Fluentd -> Kibana -> Elasticsearch" -ForegroundColor Cyan
Write-Host "             -> Python -> Chroma" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  每个服务先向上查最顶层 cmd.exe (黑窗口)"
Write-Host "  再用 taskkill /T /F 连根拔起整棵进程树"
Write-Host "  (cmd.exe + java/python/node + 黑窗口 一起消失)"
Write-Host "========================================================"

# ============================================================
# 1) 关闭 Fluentd
# ============================================================
Write-Step 1 "关闭 Fluentd (日志采集器)"
Stop-Step -stepName "Fluentd" -port $PORT_FLUENTD -windowPattern $WINDOW_FLUENTD -waitSec 2

# ============================================================
# 2) 关闭 Kibana
# ============================================================
Write-Step 2 "关闭 Kibana (日志可视化)"
Stop-Step -stepName "Kibana" -port $PORT_KIBANA -windowPattern $WINDOW_KIBANA -waitSec 3

# ============================================================
# 3) 关闭 Elasticsearch
# ============================================================
Write-Step 3 "关闭 Elasticsearch (日志存储)"
Stop-Step -stepName "Elasticsearch" -port $PORT_ES -windowPattern $WINDOW_ES -waitSec 4

# ============================================================
# 4) 关闭 Python 向量服务
# ============================================================
Write-Step 4 "关闭 Python 向量服务 (FastAPI)"
Stop-Step -stepName "Python 向量服务" -port $PORT_PYTHON -windowPattern $WINDOW_PYTHON -waitSec 2

# ============================================================
# 5) 关闭 Chroma 向量数据库
# ============================================================
Write-Step 5 "关闭 Chroma 向量数据库"
Stop-Step -stepName "Chroma" -port $PORT_CHROMA -windowPattern $WINDOW_CHROMA -waitSec 2

# ============================================================
# 收尾: 最终端口检查
# ============================================================
Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   关闭流程结束, 最终端口状态检查..." -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$portStatus = @(
    @{ Name = "Fluentd";        Port = $PORT_FLUENTD },
    @{ Name = "Kibana";         Port = $PORT_KIBANA  },
    @{ Name = "Elasticsearch";  Port = $PORT_ES      },
    @{ Name = "Python 服务";    Port = $PORT_PYTHON  },
    @{ Name = "Chroma";         Port = $PORT_CHROMA  }
)

$anyStillListening = $false
foreach ($item in $portStatus) {
    $listening = Test-PortListening $item.Port
    if ($listening) {
        Write-Host ("      {0,-18} :{1,-5} [仍监听]" -f $item.Name, $item.Port) -ForegroundColor Red
        $anyStillListening = $true
    } else {
        Write-Host ("      {0,-18} :{1,-5} [已停止]" -f $item.Name, $item.Port) -ForegroundColor Green
    }
}

if ($anyStillListening) {
    Write-Host ""
    Write-Warn "仍有端口在监听, 可能是进程被系统服务接管 (如 ES 已注册为 Windows 服务)"
    Write-Host "  如需彻底停止, 可执行:" -ForegroundColor Yellow
    Write-Host "    Stop-Service elasticsearch (如果是服务)" -ForegroundColor Gray
    Write-Host "    或: taskkill /F /PID <PID>" -ForegroundColor Gray
} else {
    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host "   全部非核心组件已停止, 黑窗口已全部关闭" -ForegroundColor Green
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host "  核心组件 (MySQL/Spring Boot) 不受影响, 可继续运行" -ForegroundColor Gray
}

Write-Host ""
Read-Host "按回车键关闭本主控窗口 (或直接点 X)"
exit 0