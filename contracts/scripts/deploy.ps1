# AIGC-LifeChain 链码部署 (PowerShell 包装)
#
# 用法 (在项目根目录下):
#   .\contracts\scripts\deploy.ps1                       # 升级全部 5 个链码
#   .\contracts\scripts\deploy.ps1 did claim             # 只升级指定链码
#   .\contracts\scripts\deploy.ps1 --init                # 首次部署
#   .\contracts\scripts\deploy.ps1 --version 1.2 did     # 指定版本号
#
# 要求: WSL 已安装且 fabric 网络容器在运行

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$WslScript = (wsl -e wslpath -a "$ScriptDir\fabric-deploy.sh").Trim()
if ($LASTEXITCODE -ne 0) { throw "无法转换项目脚本路径" }

Write-Host "[deploy] 通过 WSL 执行项目链码部署脚本" -ForegroundColor Cyan
# 逐项传参，不把用户参数拼接到 shell 命令中。
wsl -e bash $WslScript @args

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[deploy] 完成" -ForegroundColor Green
} else {
    Write-Host "`n[deploy] 失败 (exit code: $LASTEXITCODE)" -ForegroundColor Red
    exit $LASTEXITCODE
}
