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

  # MinIO 初始化：建桶 + 上传 seed-assets（封面 → public 桶；作品原件 → 私有桶）
  # 通过 lifechain-minio 容器内置的 mc 执行，不依赖宿主机 mc
  MINIO_AK="${MINIO_ACCESS_KEY:-minioadmin}"
  MINIO_SK="${MINIO_SECRET_KEY:-minioadmin}"
  BUCKET="${MINIO_BUCKET:-lifechain}"
  PUBLIC_BUCKET="${MINIO_PUBLIC_BUCKET:-lifechain-public}"
  SEED_ASSETS_HOST="$SCRIPT_DIR/../seed-assets"
  MINIO_CONTAINER="${MINIO_CONTAINER:-lifechain-minio}"

  if docker ps --format '{{.Names}}' | grep -q "^${MINIO_CONTAINER}$"; then
    log "初始化 MinIO 桶"
    # MSYS_NO_PATHCONV=1 防止 git bash 把容器内 / 路径错误转成 Windows 路径
    MSYS_NO_PATHCONV=1 docker exec "$MINIO_CONTAINER" mc alias set lifelocal http://localhost:9000 "$MINIO_AK" "$MINIO_SK" >/dev/null 2>&1
    MSYS_NO_PATHCONV=1 docker exec "$MINIO_CONTAINER" mc rm --recursive --force "lifelocal/${BUCKET}/work/" >/dev/null 2>&1 || true
    MSYS_NO_PATHCONV=1 docker exec "$MINIO_CONTAINER" mc rm --recursive --force "lifelocal/${PUBLIC_BUCKET}/cover/" >/dev/null 2>&1 || true
    MSYS_NO_PATHCONV=1 docker exec "$MINIO_CONTAINER" mc mb --ignore-existing "lifelocal/${BUCKET}" >/dev/null 2>&1
    MSYS_NO_PATHCONV=1 docker exec "$MINIO_CONTAINER" mc mb --ignore-existing "lifelocal/${PUBLIC_BUCKET}" >/dev/null 2>&1
    MSYS_NO_PATHCONV=1 docker exec "$MINIO_CONTAINER" mc anonymous set download "lifelocal/${PUBLIC_BUCKET}" >/dev/null 2>&1
    ok "桶就绪 ($BUCKET + $PUBLIC_BUCKET，public 桶已开匿名读)"

    if [[ -d "$SEED_ASSETS_HOST" ]]; then
      log "上传 seed-assets 到 MinIO"
      MSYS_NO_PATHCONV=1 docker exec "$MINIO_CONTAINER" rm -rf /tmp/seed-assets >/dev/null 2>&1 || true
      MSYS_NO_PATHCONV=1 docker exec "$MINIO_CONTAINER" mkdir -p /tmp/seed-assets >/dev/null 2>&1
      # 在 git bash / MSYS 下 docker cp 的 Linux 风格路径会被 MSYS 转换；用 cygpath 转 Windows 路径再传
      ABS_SEED=$(cd "$SEED_ASSETS_HOST" && pwd)
      if command -v cygpath >/dev/null 2>&1; then
        ABS_SEED_NATIVE=$(cygpath -w "$ABS_SEED")
      else
        ABS_SEED_NATIVE="$ABS_SEED"
      fi
      [[ -d "$ABS_SEED/work"  ]] && docker cp "${ABS_SEED_NATIVE}\\work"  "${MINIO_CONTAINER}:/tmp/seed-assets/work"  >/dev/null
      [[ -d "$ABS_SEED/cover" ]] && docker cp "${ABS_SEED_NATIVE}\\cover" "${MINIO_CONTAINER}:/tmp/seed-assets/cover" >/dev/null
      [[ -d "$ABS_SEED/work"  ]] && MSYS_NO_PATHCONV=1 docker exec "$MINIO_CONTAINER" mc cp --recursive /tmp/seed-assets/work/  "lifelocal/${BUCKET}/work/"  >/dev/null 2>&1
      [[ -d "$ABS_SEED/cover" ]] && MSYS_NO_PATHCONV=1 docker exec "$MINIO_CONTAINER" mc cp --recursive /tmp/seed-assets/cover/ "lifelocal/${PUBLIC_BUCKET}/cover/" >/dev/null 2>&1
      MSYS_NO_PATHCONV=1 docker exec "$MINIO_CONTAINER" rm -rf /tmp/seed-assets >/dev/null 2>&1 || true
      WORK_FILES=$(find "$SEED_ASSETS_HOST/work"  -type f 2>/dev/null | wc -l | tr -d ' ')
      COVER_FILES=$(find "$SEED_ASSETS_HOST/cover" -type f 2>/dev/null | wc -l | tr -d ' ')
      ok "seed-assets 上传完成（work=$WORK_FILES, cover=$COVER_FILES）"
    else
      echo "  seed-assets 目录不存在，跳过上传: $SEED_ASSETS_HOST"
    fi
  else
    echo "  容器 $MINIO_CONTAINER 未运行，跳过 MinIO 初始化"
  fi
fi

log "完成"
