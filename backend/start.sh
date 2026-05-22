#!/usr/bin/env bash
# AIGC-LifeChain 后端启动脚本
#
# 用法 (在 backend 目录下):
#   bash start.sh              # 编译打包并启动（默认）
#   bash start.sh --skip-build # 跳过编译，直接运行已有 JAR

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
JAR_FILE="$SCRIPT_DIR/lifechain-app/target/lifechain-app-1.0.0.jar"

SKIP_BUILD=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build) SKIP_BUILD=true; shift ;;
    *) echo "未知参数: $1"; exit 1 ;;
  esac
done

if [[ ! -f "$ENV_FILE" ]]; then
  echo "未找到 $ENV_FILE, 请基于 .env.example 创建"
  exit 1
fi

set -a
# shellcheck disable=SC1090
source <(grep -v '^\s*#' "$ENV_FILE" | grep -v '^\s*$')
set +a

cd "$SCRIPT_DIR"

if [[ "$SKIP_BUILD" == "false" ]]; then
  echo "[start] mvn 编译打包中..."
  mvn -pl lifechain-app -am clean package -DskipTests -q
fi

if [[ ! -f "$JAR_FILE" ]]; then
  echo "未找到 $JAR_FILE, 请先不带 --skip-build 执行"
  exit 1
fi

echo "[start] 启动后端服务"
exec java -jar "$JAR_FILE"
