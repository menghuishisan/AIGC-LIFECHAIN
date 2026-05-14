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
┌─────────────────────────────────────────────────────────┐
│                     Frontend (Vue 3)                      │
│          Portal / Admin / Regulator / Verify             │
├─────────────────────────────────────────────────────────┤
│                  Backend (Spring Boot 3)                  │
│    Auth │ Work │ Trade │ Settlement │ Regulator │ Chain   │
├─────────────────────────────────────────────────────────┤
│               Hyperledger Fabric 2.5                     │
│    DID │ Claim │ License │ Settlement │ Regulatory       │
├─────────────────────────────────────────────────────────┤
│          Infrastructure (MySQL / Redis / MinIO)          │
└─────────────────────────────────────────────────────────┘
```

| 层级 | 技术栈 |
|------|--------|
| 前端 | Vue 3.4 + TypeScript 5 + Element Plus 2.6 + Vite 5 + Pinia + ECharts |
| 后端 | Java 21 + Spring Boot 3.2 + MyBatis Plus 3.5 + SpringDoc OpenAPI |
| 区块链 | Hyperledger Fabric 2.5 + Go 1.21 链码 + fabric-contract-api-go |
| 存储 | MySQL 8.0 + Redis 7 + MinIO |
| 支付 | 微信支付 V3 + 支付宝 |

## 目录结构

```
AIGC-LifeChain/
├── frontend/                  # 前端工程
│   ├── src/
│   │   ├── app/               # 应用入口 & 路由
│   │   ├── apps/              # 多应用子目录
│   │   │   ├── portal/        # 创作者门户（作品管理、确权、交易）
│   │   │   ├── admin/         # 平台管理后台（用户、审核、数据）
│   │   │   ├── regulator/     # 监管端（冻结、争议、报告）
│   │   │   └── verify/        # 公开验证页（证书验真、链上查询）
│   │   └── shared/            # 共享组件、工具、常量
│   ├── .env.example           # 前端环境变量模板
│   └── vite.config.ts         # Vite 配置（代理、分包）
├── backend/                   # 后端工程（Maven 多模块）
│   ├── lifechain-app/         # 启动模块 & 配置 & .env.example
│   ├── lifechain-auth/        # 认证授权（JWT、角色、权限）
│   ├── lifechain-work/        # 作品管理（上传、元数据、确权申请）
│   ├── lifechain-trade/       # 交易管理（挂牌、订单、支付）
│   ├── lifechain-settlement/  # 结算管理（分账、逆分账）
│   ├── lifechain-regulator/   # 监管模块（冻结、争议、报告）
│   ├── lifechain-chain/       # 区块链对接层（Fabric Gateway）
│   ├── lifechain-common/      # 公共组件（枚举、异常、工具）
│   ├── lifechain-infra/       # 基础设施（MinIO、Redis、短信）
│   ├── runtime-config/        # Fabric 运行时配置（证书，不入库）
│   └── sql/                   # 数据库迁移 & 重置脚本
├── contracts/                 # Hyperledger Fabric 智能合约（Go）
│   ├── did-chaincode/         # DID 身份链码
│   ├── claim-chaincode/       # 确权链码
│   ├── license-chaincode/     # 授权许可链码
│   ├── settlement-chaincode/  # 结算链码
│   ├── regulatory-chaincode/  # 监管链码
│   └── scripts/               # 链码部署脚本（bash + PowerShell）
└── infra/                     # 本地开发基础设施
    └── docker-compose.yml     # MySQL + Redis + MinIO 容器编排
```

## 快速开始

### 环境要求

| 工具 | 版本 | 用途 |
|------|------|------|
| Java | 21+ | 后端编译运行 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| pnpm | 8+ | 前端包管理 |
| Go | 1.21+ | 链码编译 |
| Docker | 24+ | 基础设施 & Fabric 网络 |
| Docker Compose | v2 | 容器编排 |
| WSL 2 | - | Windows 用户运行 Fabric |

### 1. 启动基础设施（MySQL / Redis / MinIO）

```bash
cd infra
docker compose up -d
```

容器启动后的访问地址：

| 服务 | 地址 | 默认账号 |
|------|------|----------|
| MySQL | localhost:3307 | root / 123456 |
| Redis | localhost:6380 | 无密码 |
| MinIO Console | http://localhost:9021 | minioadmin / minioadmin |
| MinIO API | http://localhost:9020 | - |

### 2. 初始化数据库

```powershell
# PowerShell（删库 → 建库 → 迁移 → 种子数据）
.\backend\sql\reset-db.ps1

# 只重灌种子数据（不删库）
.\backend\sql\reset-db.ps1 -SeedOnly
```

```bash
# bash / WSL
bash backend/sql/reset-db.sh
bash backend/sql/reset-db.sh --seed-only
```

种子数据包含 5 个测试账号（密码均为 `123456`）：

| 账号 | 角色 | 用途 |
|------|------|------|
| platform_admin | 平台管理员 | Admin 后台 |
| creator_alice | 创作者 | Portal 门户 |
| creator_bob | 创作者 | Portal 门户 |
| buyer_carol | 购买者 | 交易测试 |
| regulator_dave | 监管员 | Regulator 端 |

### 3. 配置后端环境变量

```bash
cd backend/lifechain-app
cp .env.example .env
# 编辑 .env，填入实际值（使用 infra 容器时注意端口）
```

使用 `infra/docker-compose.yml` 时的关键配置：

```properties
DB_PASSWORD=123456
REDIS_PORT=6380
MINIO_ENDPOINT=http://localhost:9020
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

### 4. 启动区块链网络（可选）

不对接区块链时后端仍可启动，链上操作会报错但不影响其他功能。

```bash
# WSL 中执行
cd ~/fabric/lifechain-network/docker
docker compose up -d

# 首次部署链码
cd /mnt/e/code/AIGC-LifeChain/contracts/scripts
bash fabric-deploy.sh --init
```

后续链码升级（修改源码后）：

```powershell
# Windows PowerShell
.\contracts\scripts\deploy.ps1 did claim    # 指定链码
.\contracts\scripts\deploy.ps1              # 全部
```

### 5. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar lifechain-app/target/lifechain-app-1.0.0.jar
```

后端启动后：
- API 服务：http://localhost:8080
- Swagger UI：http://localhost:8080/swagger-ui.html（需 `SWAGGER_ENABLED=true`）

### 6. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端开发服务器：http://localhost:3000（自动代理 `/api` 到后端 8080）

四个子应用通过路由区分：
- `/portal` — 创作者门户
- `/admin` — 平台管理后台
- `/regulator` — 监管端
- `/verify` — 公开验证页

## 智能合约

5 个 Go 链码部署在 Hyperledger Fabric `lifechainchannel` 通道上：

| 链码 | 功能 | 主要接口 |
|------|------|----------|
| `did_chaincode` | 身份管理 | RegisterDID, SuspendDID, RevokeDID, QueryDID |
| `claim_chaincode` | 内容确权 | RegisterClaim, QueryClaim |
| `license_chaincode` | 授权许可 | RegisterLicense, QueryLicense |
| `settlement_chaincode` | 结算存证 | RegisterSettlement, RegisterReverseSettlement, QuerySettlement |
| `regulatory_chaincode` | 监管治理 | RegisterFreeze, RegisterUnfreeze, RegisterDisputeConclusion, RegisterReport |

所有链码支持 `GetHistoryByKey` 查询完整变更历史。

账本 Key 格式：

| 链码 | Key |
|------|-----|
| did | `DID#{didNo}` |
| claim | `CLAIM#{claimNo}` |
| license | `LICENSE#{licenseNo}` |
| settlement | `SETTLE#{settleNo}` / `REVERSE_SETTLE#{reverseSettleNo}` |
| regulatory | `FREEZE#{freezeNo}` / `DISPUTE#{caseNo}` / `REPORT#{reportNo}` |

## 开发说明

### 后端

- **配置**：所有可配置项通过环境变量注入，模板见 `backend/lifechain-app/.env.example`
- **API 文档**：`SWAGGER_ENABLED=true` 后访问 `/swagger-ui.html`
- **数据库迁移**：`backend/sql/V*__*.sql` 按版本号顺序执行
- **数据库重置**：`backend/sql/reset-db.ps1`（PowerShell）或 `reset-db.sh`（bash）
- **日志级别**：业务代码 DEBUG，框架 INFO，可在 `application.yml` 调整

### 前端

- **配置**：环境变量模板见 `frontend/.env.example`
- **开发代理**：`vite.config.ts` 中 `/api` 和 `/public` 代理到 `localhost:8080`
- **多应用**：`src/apps/` 下 portal / admin / regulator / verify 四个子应用共享 `src/shared/`
- **UI 组件**：Element Plus 按需自动导入，无需手动 import
- **构建**：`pnpm build` 产出到 `dist/`，自动分包（echarts / element-plus / vue 独立 chunk）

### 链码

- **开发**：修改 `contracts/` 下 Go 源码
- **编译检查**：`cd contracts && make build`
- **部署**：`.\contracts\scripts\deploy.ps1 <chaincode-name>`（自动 vendor → package → install → approve → commit）
- **升级机制**：脚本自动检测当前 sequence 并 +1，无需手动管理版本号

## License

MIT
