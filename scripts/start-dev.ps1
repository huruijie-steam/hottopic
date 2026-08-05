# 一键启动开发环境(后端 :3001 + 前端 :5173)
# 用法:.\scripts\start-dev.ps1 [-SkipFrontend]

param(
    [switch]$SkipFrontend
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

Write-Host "🔧 检查环境..." -ForegroundColor Cyan
if (-not $env:DEEPSEEK_API_KEY) {
    Write-Host "⚠️  未设置 DEEPSEEK_API_KEY,AI 分析将使用降级模式(文本预匹配)" -ForegroundColor Yellow
}

# 启动后端
Write-Host "🚀 启动后端 (:3001)..." -ForegroundColor Cyan
Push-Location $root
$backend = Start-Process powershell -ArgumentList "-NoExit", "-Command", ".\mvnw.cmd spring-boot:run" -PassThru
Pop-Location
Write-Host "   后端 PID: $($backend.Id) (日志在后台终端)" -ForegroundColor Gray

# 启动前端
if (-not $SkipFrontend) {
    Write-Host "🚀 启动前端 (:5173)..." -ForegroundColor Cyan
    Push-Location "$root\client"
    if (-not (Test-Path node_modules)) {
        Write-Host "   首次运行,安装前端依赖..." -ForegroundColor Gray
        npm install
    }
    $frontend = Start-Process powershell -ArgumentList "-NoExit", "-Command", "npm run dev" -PassThru
    Pop-Location
    Write-Host "   前端 PID: $($frontend.Id)" -ForegroundColor Gray
} else {
    Write-Host "⏭  跳过前端" -ForegroundColor Gray
}

Write-Host ""
Write-Host "✅ 已启动。访问:" -ForegroundColor Green
Write-Host "   前端: http://localhost:5173"
Write-Host "   后端: http://localhost:3001/api/health"
Write-Host "   关闭: 直接关闭对应终端窗口即可"
