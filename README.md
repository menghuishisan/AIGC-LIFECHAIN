# AIGC-LifeChain

基于区块链的 AIGC 内容可信管理平台，为 AI 生成内容提供全生命周期的确权、授权、交易与监管能力。

## 项目简介

AIGC-LifeChain 围绕 AI 生成内容（文本、图像、音视频等）构建了一套完整的可信管理体系：

- **DID 身份** — 基于 W3C DID 规范，为创作者和机构颁发链上数字身份
- **内容确权** — 对 AIGC 作品进行哈希存证与链上登记，生成不可篡改的权属证明
- **授权许可** — 支持多种授权模式（独占/非独占/转授权），合同关键条款上链
- **交易结算** — 集成微信/支付宝支付，分账结算记录全程上链可追溯
- **监管合规** — 提供冻结/解冻、争议仲裁、监管报告等链上治理工具

## 技术架构

```
┌─────────────────────────────────────────────────────┐
│                    Frontend (Vue 3)                   │
│         Portal / Admin / Regulator / Verify          │
├─────────────────────────────────────────────────────┤
│                 Backend (Spring Boot 3)               │
│   Auth │ Work │ Trade │ Settlement │ Regulator │ Chain│
├─────────────────────────────────────────────────────┤
│              Hyperledger Fabric 2.5                   │
│   DID │ Claim │ License │ Settlement │ Regulatory    │
└─────────────────────────────────────────────────────┘
```

| 层级 | 技术栈 |
|------|--------|
| 前端 | Vue 3 + TypeScript + Element Plus + Vite |
| 后端 | Java 21 + Spring Boot 3.2 + MyBatis Plus |
| 区块链 | Hyperledger Fabric 2.5 + Go 链码 |
| 存储 | MySQL 8.0 + Redis + MinIO |
| 支付 | 微信支付 + 支付宝 |

## 目录结构

```
AIGC-LifeChain/
├── frontend/                # 前端工程（多应用：portal/admin/regulator/verify）
├── backend/                 # 后端工程（Maven 多模块）
│   ├── lifechain-app/       # 启动模块 & 配置
│   ├── lifechain-auth/      # 认证授权
│   ├── lifechain-work/      # 作品管理
│   ├── lifechain-trade/     # 交易管理
│   ├── lifechain-settlement/# 结算管理
│   ├── lifechain-regulator/ # 监管模块
│   ├── lifechain-chain/     # 区块链对接层
│   ├── lifechain-common/    # 公共组件
│   ├── lifechain-infra/     # 基础设施
│   ├── runtime-config/      # 运行时配置（证书等，不入库）
│   └── sql/                 # 数据库迁移脚本
└── contracts/               # Hyperledger Fabric 智能合约
    ├── did-chaincode/       # DID 身份链码
    ├── claim-chaincode/     # 确权链码
    ├── license-chaincode/   # 授权许可链码
    ├── settlement-chaincode/# 结算链码
    ├── regulatory-chaincode/# 监管链码
    └── scripts/             # 部署脚本
```

## 快速开始

### 环境要求

- Java 21+
- Node.js 18+ / pnpm
- Go 1.21+
- Docker & Docker Compose
- WSL 2（Windows 用户）
- Hyperledger Fabric 2.5 二进制工具（peer, cryptogen, configtxgen）

### 1. 启动区块链网络

```bash
# WSL 中执行（首次）
cd ~/fabric/lifechain-network/docker
docker compose up -d

# 创建通道 & 部署链码
cd /mnt/e/code/AIGC-LifeChain/contracts/scripts
bash fabric-deploy.sh --init
```

后续链码升级：

```powershell
# Windows PowerShell（项目目录下）
.\contracts\scripts\deploy.ps1 did claim
```

### 2. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar lifechain-app/target/lifechain-app-1.0.0.jar
```

### 3. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

## 智能合约

5 个 Go 链码部署在 Hyperledger Fabric `lifechainchannel` 通道上：

| 链码 | 功能 | 主要接口 |
|------|------|----------|
| `did_chaincode` | 身份管理 | RegisterDID, SuspendDID, RevokeDID, QueryDID |
| `claim_chaincode` | 内容确权 | RegisterClaim, QueryClaim |
| `license_chaincode` | 授权许可 | RegisterLicense, QueryLicense |
| `settlement_chaincode` | 结算存证 | RegisterSettlement, RegisterReverseSettlement |
| `regulatory_chaincode` | 监管治理 | RegisterFreeze, RegisterUnfreeze, RegisterDisputeConclusion |

所有链码支持 `GetHistoryByKey` 查询完整变更历史。

## 开发说明

- 后端 API 文档：启动后访问 `/swagger-ui.html`
- 数据库迁移：`backend/sql/` 下按版本号顺序执行
- 链码开发：修改 `contracts/` 下源码后执行 `deploy.ps1` 即可热升级
- 前端多应用：`frontend/src/apps/` 下分 portal / admin / regulator / verify 四个子应用

## License

MIT
