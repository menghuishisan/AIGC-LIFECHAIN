# AIGC-LifeChain 数据库重置 (PowerShell)
#
# 用法 (在项目根目录下):
#   .\backend\sql\reset-db.ps1              # 删库 → 建库 → 迁移 → 种子数据
#   .\backend\sql\reset-db.ps1 -NoSeed      # 不灌种子数据
#   .\backend\sql\reset-db.ps1 -SeedOnly    # 只灌种子数据
#
# 通过 docker exec 在 lifechain-mysql 容器内执行，无需本地安装 mysql 客户端

param(
    [switch]$NoSeed,
    [switch]$SeedOnly
)

# 加载 .env
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile = Join-Path $ScriptDir "..\.env"
if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            [System.Environment]::SetEnvironmentVariable($Matches[1].Trim(), $Matches[2].Trim(), "Process")
        }
    }
}

$DB_NAME = if ($env:DB_NAME) { $env:DB_NAME } else { "lifechain" }
$DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }
$CONTAINER = if ($env:DB_CONTAINER) { $env:DB_CONTAINER } else { "lifechain-mysql" }

function Invoke-Sql($sql) {
    docker exec $CONTAINER mysql -u root -p"$DB_PASSWORD" -e "$sql" 2>$null
    if ($LASTEXITCODE -ne 0) { Write-Host "SQL 执行失败: $sql" -ForegroundColor Red; exit 1 }
}

function Invoke-SqlFile($file) {
    docker cp $file "${CONTAINER}:/tmp/exec.sql" 2>$null
    docker exec $CONTAINER mysql -u root -p"$DB_PASSWORD" $DB_NAME -e "source /tmp/exec.sql" 2>$null
    if ($LASTEXITCODE -ne 0) { Write-Host "执行失败: $file" -ForegroundColor Red; exit 1 }
}

# 检查容器是否运行
$running = docker ps --format "{{.Names}}" | Select-String -Pattern "^${CONTAINER}$"
if (-not $running) {
    Write-Host "容器 $CONTAINER 未运行，请先: cd infra && docker compose --env-file ../backend/lifechain-app/.env up -d" -ForegroundColor Red
    exit 1
}

if (-not $SeedOnly) {
    Write-Host "[reset-db] 删除数据库 $DB_NAME" -ForegroundColor Cyan
    Invoke-Sql "DROP DATABASE IF EXISTS ``$DB_NAME``;"

    Write-Host "[reset-db] 创建数据库 $DB_NAME" -ForegroundColor Cyan
    Invoke-Sql "CREATE DATABASE ``$DB_NAME`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

    Write-Host "[reset-db] 执行迁移脚本" -ForegroundColor Cyan
    Get-ChildItem "$ScriptDir\V*.sql" | Sort-Object Name | ForEach-Object {
        Invoke-SqlFile $_.FullName
        Write-Host "  OK $($_.Name)" -ForegroundColor Green
    }
}

if (-not $NoSeed) {
    Write-Host "[reset-db] 灌入种子数据" -ForegroundColor Cyan
    $seedFile = Join-Path $ScriptDir "seed_testdata_chain.sql"
    if (Test-Path $seedFile) {
        Invoke-SqlFile $seedFile
        Write-Host "  OK seed_testdata_chain.sql" -ForegroundColor Green
    } else {
        Write-Host "  种子文件不存在: $seedFile" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n[reset-db] 完成" -ForegroundColor Green
