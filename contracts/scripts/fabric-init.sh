#!/usr/bin/env bash
# 仅在空目录生成本地开发网络素材；已存在的身份、通道和账本一律保留。
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
NET_DIR="${NET_DIR:-$PROJECT_DIR/infra/fabric}"
for tool in cryptogen configtxgen; do command -v "$tool" >/dev/null || { echo "缺少工具: $tool" >&2; exit 1; }; done
if [[ -e "$NET_DIR/crypto-config" || -e "$NET_DIR/channel-artifacts" ]]; then
  echo "网络素材已存在，拒绝覆盖。已有网络直接使用 fabric-deploy.sh。" >&2
  exit 1
fi
umask 077
STAGING="$(mktemp -d "$NET_DIR/.assets-XXXXXX")"
trap 'rm -rf -- "$STAGING"' EXIT
cp "$NET_DIR/configtx.yaml" "$STAGING/configtx.yaml"
cryptogen generate --config="$NET_DIR/crypto-config.yaml" --output="$STAGING/crypto-config"
mkdir "$STAGING/channel-artifacts"
export FABRIC_CFG_PATH="$STAGING"
configtxgen -profile LifeChainOrdererGenesis -channelID lifechainsystem -outputBlock "$STAGING/channel-artifacts/genesis.block"
configtxgen -profile LifeChainChannel -channelID lifechainchannel -outputCreateChannelTx "$STAGING/channel-artifacts/lifechainchannel.tx"
for org in Org1MSP Org2MSP; do
  configtxgen -profile LifeChainChannel -channelID lifechainchannel -asOrg "$org" -outputAnchorPeersUpdate "$STAGING/channel-artifacts/${org}anchors.tx"
done
# cryptogen 私钥名含随机后缀，部署配置统一使用固定的本地文件名。
while IFS= read -r -d '' key; do mv -- "$key" "$(dirname -- "$key")/priv_sk"; done < <(find "$STAGING/crypto-config" -name '*_sk' ! -name priv_sk -print0)
mv -- "$STAGING/crypto-config" "$NET_DIR/crypto-config"
mv -- "$STAGING/channel-artifacts" "$NET_DIR/channel-artifacts"
echo "开发网络素材已生成；请启动项目 Compose，再部署链码。"
