# 打包 + 前端静态托管:构建前后端为单个可运行 jar
# 用法:.\scripts\build-release.ps1
# 产物:target\HotTopic-0.0.1-SNAPSHOT.jar(包含前端页面,单端口 :3001)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

Write-Host "🧹 清理旧构建..." -ForegroundColor Cyan
Remove-Item -Recurse -Force "$root\src\main\resources\static" -ErrorAction SilentlyContinue

Write-Host "🏗  构建前端 (client → static)..." -ForegroundColor Cyan
Push-Location "$root\client"
if (-not (Test-Path node_modules)) { npm install }
npm run build
$dist = "$root\src\main\resources\static"
New-Item -ItemType Directory -Path $dist -Force | Out-Null
Copy-Item -Recurse -Force "$root\client\dist\*" $dist
Pop-Location
Write-Host "   前端已拷贝至 src/main/resources/static" -ForegroundColor Gray

Write-Host "🏗  打包后端 jar..." -ForegroundColor Cyan
Push-Location $root
.\mvnw.cmd package -DskipTests
Pop-Location

Write-Host ""
Write-Host "✅ 构建完成:" -ForegroundColor Green
Write-Host "   $root\target\HotTopic-0.0.1-SNAPSHOT.jar"
Write-Host ""
Write-Host "运行:"
Write-Host "   java -jar target\HotTopic-0.0.1-SNAPSHOT.jar"
Write-Host "访问: http://localhost:3001 (页面 + API 同端口)"
