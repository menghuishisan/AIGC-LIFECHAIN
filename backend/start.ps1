# AIGC-LifeChain 后端启动脚本 (PowerShell)
#
# 用法 (在 backend 目录或项目根目录均可):
#   .\backend\start.ps1            # 加载 backend\.env 并启动 lifechain-app
#   .\backend\start.ps1 -Build     # 启动前先 mvn 重新打包
#
# 设计原则 (12-factor):
#   - 应用本身只读环境变量, 不感知 .env 文件
#   - 本脚本读取 .env 后通过 SetEnvironmentVariable 注入进程环境
#   - 生产部署时改用 systemd EnvironmentFile= / docker-compose env_file 即可, 应用零改动

param(
    [switch]$Build
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

if ($Build) {
    Write-Host "[start] mvn 打包中..."
    Push-Location $ScriptDir
    try {
        mvn -pl lifechain-app -am clean package -DskipTests -q
        if ($LASTEXITCODE -ne 0) { throw "Maven 打包失败" }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $JarFile)) {
    Write-Error "未找到 $JarFile, 请先执行: .\backend\start.ps1 -Build"
    exit 1
}

Set-Location $ScriptDir
& java -jar $JarFile
