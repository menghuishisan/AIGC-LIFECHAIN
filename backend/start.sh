#!/usr/bin/env bash
# AIGC-LifeChain 后端启动脚本
#
# 用法:
#   bash start.sh                # 加载 backend/.env 并启动 lifechain-app
#   bash start.sh --build        # 启动前先 mvn 重新打包
#
# 设计原则 (12-factor):
#   - 应用本身只读环境变量, 不感知 .env 文件
#   - 本脚本读取 .env 后通过 export 注入进程环境
#   - 生产部署时改用 systemd EnvironmentFile= / docker-compose env_file 即可, 应用零改动

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
JAR_FILE="$SCRIPT_DIR/lifechain-app/target/lifechain-app-1.0.0.jar"

DO_BUILD=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --build) DO_BUILD=true; shift ;;
    *) echo "未知参数: $1"; exit 1 ;;
  esac
done

if [[ ! -f "$ENV_FILE" ]]; then
  echo "未找到 $ENV_FILE, 请基于 .env.example 创建"
  exit 1
fi

# 把 .env 的 KEY=VALUE 注入当前 shell 的环境变量
set -a
# shellcheck disable=SC1090
source <(grep -v '^\s*#' "$ENV_FILE" | grep -v '^\s*$')
set +a

if [[ "$DO_BUILD" == "true" ]]; then
  echo "[start] mvn 打包中..."
  (cd "$SCRIPT_DIR" && mvn -pl lifechain-app -am clean package -DskipTests -q)
fi

if [[ ! -f "$JAR_FILE" ]]; then
  echo "未找到 $JAR_FILE, 请先执行: bash start.sh --build"
  exit 1
fi

cd "$SCRIPT_DIR"
exec java -jar "$JAR_FILE"
