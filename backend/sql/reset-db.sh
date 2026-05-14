#!/usr/bin/env bash
# AIGC-LifeChain 数据库重置脚本
#
# 用法:
#   bash reset-db.sh              # 删库 → 建库 → 跑迁移 → 灌种子数据
#   bash reset-db.sh --no-seed    # 不灌种子数据
#   bash reset-db.sh --seed-only  # 只灌种子数据（不删库重建）
#
# 通过 docker exec 在 lifechain-mysql 容器内执行，无需本地安装 mysql 客户端
# 密码从 backend/lifechain-app/.env 的 DB_PASSWORD 读取

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SQL_DIR="$SCRIPT_DIR"

# 加载 .env
ENV_FILE="$SCRIPT_DIR/../.env"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  source <(grep -v '^\s*#' "$ENV_FILE" | grep -v '^\s*$')
  set +a
fi

DB_NAME="${DB_NAME:-lifechain}"
DB_PASSWORD="${DB_PASSWORD:-123456}"
CONTAINER="${DB_CONTAINER:-lifechain-mysql}"

SEED_ONLY=false
NO_SEED=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --seed-only) SEED_ONLY=true; shift ;;
    --no-seed)   NO_SEED=true; shift ;;
    *) echo "未知参数: $1"; exit 1 ;;
  esac
done

run_sql() {
  docker exec -i "$CONTAINER" mysql -u root -p"$DB_PASSWORD" "$@"
}

log() { echo -e "\033[36m[$(date +%H:%M:%S)]\033[0m $*"; }
ok()  { echo -e "\033[32m  OK\033[0m $*"; }

# 检查容器是否运行
docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$" || { echo "容器 $CONTAINER 未运行，请先 cd infra && docker compose --env-file ../backend/lifechain-app/.env up -d"; exit 1; }

if [[ "$SEED_ONLY" == false ]]; then
  log "删除数据库 $DB_NAME（如果存在）"
  echo "DROP DATABASE IF EXISTS \`$DB_NAME\`;" | run_sql 2>/dev/null
  ok "已删除"

  log "创建数据库 $DB_NAME"
  echo "CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | run_sql 2>/dev/null
  ok "已创建"

  log "执行迁移脚本"
  for f in "$SQL_DIR"/V[0-9]*__*.sql; do
    [[ -f "$f" ]] || continue
    fname=$(basename "$f")
    run_sql "$DB_NAME" < "$f" 2>/dev/null
    ok "$fname"
  done
fi

if [[ "$NO_SEED" == false ]]; then
  log "灌入种子数据"
  SEED_FILE="$SQL_DIR/seed_testdata_chain.sql"
  if [[ -f "$SEED_FILE" ]]; then
    run_sql "$DB_NAME" < "$SEED_FILE" 2>/dev/null
    ok "seed_testdata_chain.sql"
  else
    echo "  种子文件不存在: $SEED_FILE"
    exit 1
  fi
fi

log "完成"
