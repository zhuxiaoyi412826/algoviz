# ============================================================
# 模拟微信公众号回调登录测试脚本
# 用法: powershell -ExecutionPolicy Bypass -File test_wechat_login.ps1
# ============================================================
$ErrorActionPreference = "Stop"
$BASE_URL = "http://localhost"
$WX_TOKEN = "zhuxiaoyizxy123456789"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  模拟微信公众号回调登录测试" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ========== Step 1: 获取验证码 ==========
Write-Host "[Step 1] 获取验证码..." -ForegroundColor Yellow
$resp = Invoke-WebRequest -Uri "$BASE_URL/api/login/verification-code" -Method GET -TimeoutSec 10 -UseBasicParsing
$code = $resp.Content.Trim()
Write-Host "  验证码: $code" -ForegroundColor Green

# ========== Step 2: 构造微信回调签名 ==========
Write-Host ""
Write-Host "[Step 2] 构造微信签名..." -ForegroundColor Yellow
$timestamp = [string][int][double]::Parse((Get-Date -UFormat %s))
$nonce = "testnonce123456"
$arr = @($WX_TOKEN, $timestamp, $nonce) | Sort-Object
$content = $arr -join ""
$sha1 = [System.Security.Cryptography.SHA1]::Create()
$hashBytes = $sha1.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($content))
$signature = ($hashBytes | ForEach-Object { $_.ToString("X2") }) -join ""
Write-Host "  token    : $WX_TOKEN"
Write-Host "  timestamp: $timestamp"
Write-Host "  nonce    : $nonce"
Write-Host "  signature: $signature"

# ========== Step 3: 模拟微信回调 POST ==========
Write-Host ""
Write-Host "[Step 3] 模拟微信回调 (POST /wx/callback)..." -ForegroundColor Yellow
$openId = "oTestUser_" + (Get-Random -Maximum 99999)
$xmlBody = '<xml><ToUserName><![CDATA[gh_algoviz]]></ToUserName><FromUserName><![CDATA[' + $openId + ']]></FromUserName><CreateTime>' + $timestamp + '</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[' + $code + ']]></Content><MsgId>1234567890123456</MsgId></xml>'

Write-Host "  openId   : $openId"
Write-Host "  发送验证码: $code"
Write-Host ""

$callbackUrl = $BASE_URL + '/wx/callback?signature=' + $signature + '&timestamp=' + $timestamp + '&nonce=' + $nonce
try {
    $response = Invoke-WebRequest -Uri $callbackUrl -Method POST -Body $xmlBody -ContentType "application/xml; charset=UTF-8" -TimeoutSec 10 -UseBasicParsing
    Write-Host "  回调响应:" -ForegroundColor Green
    Write-Host $response.Content
} catch {
    Write-Host "  回调请求失败: $_" -ForegroundColor Red
}

# ========== Step 4: 轮询 check-status 触发登录 ==========
Write-Host ""
Write-Host "[Step 4] 轮询 check-status 触发登录成功..." -ForegroundColor Yellow
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$checkUrl = $BASE_URL + '/api/login/check-status?code=' + $code
try {
    $resp2 = Invoke-WebRequest -Uri $checkUrl -Method GET -TimeoutSec 10 -WebSession $session -UseBasicParsing
    $loginResult = $resp2.Content | ConvertFrom-Json
    Write-Host "  登录结果:" -ForegroundColor Green
    $loginResult | ConvertTo-Json -Depth 5
} catch {
    Write-Host "  请求失败: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  测试完成！请查看后端控制台的卡片输出" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
