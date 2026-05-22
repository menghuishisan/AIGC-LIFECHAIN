# AIGC-LifeChain 后端启动脚本 (PowerShell)
#
# 用法 (在 backend 目录下):
#   .\start.ps1            # 编译打包并启动（默认）
#   .\start.ps1 -SkipBuild # 跳过编译，直接运行已有 JAR

param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile = Join-Path $ScriptDir ".env"
$JarFile = Join-Path $ScriptDir "lifechain-app\target\lifechain-app-1.0.0.jar"

if (-not (Test-Path $EnvFile)) {
    Write-Error "未找到 $EnvFile, 请基于 .env.example 创建"
    exit 1
}

Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^\s*([^#=][^=]*)=(.*)$') {
        $key = $Matches[1].Trim()
        $value = $Matches[2].Trim()
        if ($value.StartsWith('"') -and $value.EndsWith('"')) {
            $value = $value.Substring(1, $value.Length - 2)
        } elseif ($value.StartsWith("'") -and $value.EndsWith("'")) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
    }
}

Set-Location $ScriptDir

if (-not $SkipBuild) {
    Write-Host "[start] mvn 编译打包中..." -ForegroundColor Cyan
    mvn -pl lifechain-app -am clean package -DskipTests -q
    if ($LASTEXITCODE -ne 0) { throw "Maven 打包失败" }
}

if (-not (Test-Path $JarFile)) {
    Write-Error "未找到 $JarFile, 请先不带 -SkipBuild 执行"
    exit 1
}

Write-Host "[start] 启动后端服务" -ForegroundColor Green
& java -jar $JarFile
