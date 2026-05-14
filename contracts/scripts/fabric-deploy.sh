#!/usr/bin/env bash
# AIGC-LifeChain 链码一键部署脚本
#
# 用法：
#   ./fabric-deploy.sh                       # 升级全部 5 个链码（sequence 自动 +1）
#   ./fabric-deploy.sh did claim             # 只升级指定链码
#   ./fabric-deploy.sh --init                # 强制首次部署（sequence=1, version=1.0）
#   ./fabric-deploy.sh --version 1.2 did     # 指定版本号升级 did
#
# 仅在 WSL 中运行。
set -euo pipefail

# ---------- 路径与常量 ----------
NET_DIR="${NET_DIR:-$HOME/fabric/lifechain-network}"
CONTRACTS_DIR="${CONTRACTS_DIR:-/mnt/e/code/AIGC-LifeChain/contracts}"
CHANNEL="${CHANNEL:-lifechainchannel}"
ORDERER="${ORDERER:-localhost:7050}"
ORG1_PEER="${ORG1_PEER:-localhost:7051}"
ORG2_PEER="${ORG2_PEER:-localhost:9051}"

ALL_CCS=(did claim license settlement regulatory)

export FABRIC_CFG_PATH=/usr/local/config
export PATH=/usr/local/go/bin:$PATH
export GOWORK=off
export GOPROXY="${GOPROXY:-https://goproxy.cn,direct}"

# ---------- 参数解析 ----------
INIT_MODE=false
VERSION_OVERRIDE=""
TARGETS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --init) INIT_MODE=true; shift ;;
    --version) VERSION_OVERRIDE="$2"; shift 2 ;;
    -h|--help)
      sed -n '2,12p' "$0"; exit 0 ;;
    *) TARGETS+=("$1"); shift ;;
  esac
done
[[ ${#TARGETS[@]} -eq 0 ]] && TARGETS=("${ALL_CCS[@]}")

log()  { echo -e "\033[36m[$(date +%H:%M:%S)]\033[0m $*"; }
ok()   { echo -e "\033[32m  ✓\033[0m $*"; }
fail() { echo -e "\033[31m  ✗\033[0m $*"; exit 1; }

# ---------- 前置检查 ----------
log "检查前置依赖"
command -v peer >/dev/null || fail "peer 不在 PATH"
command -v go   >/dev/null || fail "go 不在 PATH"
command -v jq   >/dev/null || fail "jq 不在 PATH"
[[ -d "$NET_DIR" ]] || fail "网络目录不存在: $NET_DIR"
for c in orderer.lifechain.com peer0.org1.lifechain.com peer0.org2.lifechain.com; do
  docker ps --format '{{.Names}}' | grep -q "^${c}$" || fail "容器未运行: $c"
done
ok "前置依赖通过"

# ---------- 切换组织环境 ----------
use_org() {
  case "$1" in
    1)
      export CORE_PEER_LOCALMSPID=Org1MSP
      export CORE_PEER_MSPCONFIGPATH=$NET_DIR/crypto-config/peerOrganizations/org1.lifechain.com/users/Admin@org1.lifechain.com/msp
      export CORE_PEER_ADDRESS=$ORG1_PEER ;;
    2)
      export CORE_PEER_LOCALMSPID=Org2MSP
      export CORE_PEER_MSPCONFIGPATH=$NET_DIR/crypto-config/peerOrganizations/org2.lifechain.com/users/Admin@org2.lifechain.com/msp
      export CORE_PEER_ADDRESS=$ORG2_PEER ;;
  esac
}

# ---------- 单个链码部署 ----------
deploy_one() {
  local cc="$1"
  local name="${cc}_chaincode"
  local src_dir="$CONTRACTS_DIR/${cc}-chaincode"
  local pkg_path="$NET_DIR/cc-packages/${cc}-chaincode.tar.gz"

  [[ -d "$src_dir" ]] || fail "源码目录不存在: $src_dir"

  log "[${name}] vendor 依赖"
  ( cd "$src_dir" && go mod tidy >/dev/null 2>&1 && go mod vendor >/dev/null 2>&1 ) \
    || fail "vendor 失败：$src_dir"
  ok "vendor 完成"

  # 计算 sequence/version
  use_org 1
  local committed_json
  committed_json=$(peer lifecycle chaincode querycommitted --channelID "$CHANNEL" --name "$name" --output json 2>/dev/null || true)
  local cur_seq cur_ver new_seq new_ver
  if [[ -z "$committed_json" || "$INIT_MODE" == true ]]; then
    new_seq=1
    new_ver="${VERSION_OVERRIDE:-1.0}"
  else
    cur_seq=$(echo "$committed_json" | jq -r '.sequence')
    cur_ver=$(echo "$committed_json" | jq -r '.version')
    new_seq=$((cur_seq + 1))
    new_ver="${VERSION_OVERRIDE:-$cur_ver}"
  fi
  local label="${name}_${new_ver}"
  log "[${name}] 目标 version=${new_ver} sequence=${new_seq} label=${label}"

  log "[${name}] 打包"
  peer lifecycle chaincode package "$pkg_path" \
    --path "$src_dir" --lang golang --label "$label" >/dev/null
  ok "打包完成: $pkg_path"

  log "[${name}] install on org1"
  use_org 1
  peer lifecycle chaincode install "$pkg_path" >/dev/null 2>&1 || true
  ok "org1 install 完成"

  log "[${name}] install on org2"
  use_org 2
  peer lifecycle chaincode install "$pkg_path" >/dev/null 2>&1 || true
  ok "org2 install 完成"

  log "[${name}] 解析 packageID"
  use_org 1
  local pkg_id
  pkg_id=$(peer lifecycle chaincode queryinstalled --output json \
    | jq -r --arg L "$label" '.installed_chaincodes[] | select(.label==$L) | .package_id' \
    | head -1)
  [[ -n "$pkg_id" ]] || fail "找不到 packageID for label=$label"
  ok "packageID=$pkg_id"

  log "[${name}] approve org1"
  use_org 1
  peer lifecycle chaincode approveformyorg -o "$ORDERER" \
    --channelID "$CHANNEL" --name "$name" --version "$new_ver" \
    --package-id "$pkg_id" --sequence "$new_seq" >/dev/null
  ok "org1 approved"

  log "[${name}] approve org2"
  use_org 2
  peer lifecycle chaincode approveformyorg -o "$ORDERER" \
    --channelID "$CHANNEL" --name "$name" --version "$new_ver" \
    --package-id "$pkg_id" --sequence "$new_seq" >/dev/null
  ok "org2 approved"

  log "[${name}] commit"
  use_org 1
  peer lifecycle chaincode commit -o "$ORDERER" \
    --channelID "$CHANNEL" --name "$name" --version "$new_ver" --sequence "$new_seq" \
    --peerAddresses "$ORG1_PEER" \
    --peerAddresses "$ORG2_PEER" >/dev/null
  ok "commit 完成"

  log "[${name}] 验证"
  peer lifecycle chaincode querycommitted --channelID "$CHANNEL" --name "$name"
  echo
}

# ---------- 主流程 ----------
for cc in "${TARGETS[@]}"; do
  # 接受 did 或 did_chaincode 或 did-chaincode
  cc="${cc%_chaincode}"; cc="${cc%-chaincode}"
  if [[ ! " ${ALL_CCS[*]} " =~ " ${cc} " ]]; then
    fail "未知链码: $cc （可选: ${ALL_CCS[*]}）"
  fi
  deploy_one "$cc"
done

log "全部完成 ✅"
