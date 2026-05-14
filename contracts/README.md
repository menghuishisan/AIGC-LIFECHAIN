# AIGC-LifeChain 智能合约

本目录包含 AIGC-LifeChain 项目的全部 Hyperledger Fabric 链码，使用 Go 语言 + `fabric-contract-api-go v1.2.2` 框架开发。

## 目录结构

```
contracts/
├── go.work                    # Go workspace（统一管理5个模块）
├── Makefile                   # 构建脚本
├── did-chaincode/             # DID 身份链码
├── claim-chaincode/           # 声明/凭证链码
├── license-chaincode/         # 授权许可链码
├── settlement-chaincode/      # 结算链码
└── regulatory-chaincode/      # 监管链码
```

## 链码清单

| 链码名称              | 目录                    | 主要函数 |
|---------------------|------------------------|---------|
| `did_chaincode`     | did-chaincode/          | RegisterDID, SuspendDID, RevokeDID, QueryDID, GetHistoryByKey |
| `claim_chaincode`   | claim-chaincode/        | RegisterClaim, QueryClaim, GetHistoryByKey |
| `license_chaincode` | license-chaincode/      | RegisterLicense, QueryLicense, GetHistoryByKey |
| `settlement_chaincode` | settlement-chaincode/ | RegisterSettlement, RegisterReverseSettlement, QuerySettlement, QueryReverseSettlement, GetHistoryByKey |
| `regulatory_chaincode` | regulatory-chaincode/ | RegisterFreeze, RegisterUnfreeze, RegisterDisputeConclusion, RegisterReport, GetHistoryByKey |

## 账本 Key 格式

| 链码 | Key 格式 |
|-----|---------|
| did_chaincode | `DID#<didNo>` |
| claim_chaincode | `CLAIM#<claimNo>` |
| license_chaincode | `LICENSE#<licenseNo>` |
| settlement_chaincode | `SETTLE#<settleNo>` / `REVERSE_SETTLE#<reverseSettleNo>` |
| regulatory_chaincode | `FREEZE#<freezeNo>` / `DISPUTE#<disputeNo>` / `REPORT#<reportNo>` |

## 设计规范

- **幂等性**：所有 Register 函数在同一业务主键已存在时返回错误，防止重复上链
- **时间格式**：统一使用 `GetTxTimestamp()` 获取 Fabric 交易时间，格式为 UTC RFC3339
- **链码事件**：每次写入成功后通过 `SetEvent` 发布事件，payload 格式：
  ```json
  { "bizType": "DID", "bizNo": "DID202603100001", "txTime": "2026-03-10T00:00:00Z", "status": "DID_ACTIVE" }
  ```
- **GetHistoryByKey**：所有链码均支持按账本 Key 查询完整变更历史

## 构建

```bash
# 进入 contracts 目录
cd contracts

# 下载依赖
make tidy

# 编译所有链码
make build

# 静态检查
make vet

# 单元测试
make test
```

## 部署（Fabric peer CLI）

```bash
# 以 did_chaincode 为例
cd did-chaincode
peer lifecycle chaincode package did_chaincode.tar.gz \
  --path . --lang golang --label did_chaincode_1.0

peer lifecycle chaincode install did_chaincode.tar.gz

# 审批、提交流程参考 Fabric 官方文档
```

## 与后端的交互方式

后端通过 `lifechain-chain` 模块的 `FabricChainService` 调用链码：

1. 业务层先在数据库完成状态流转（中间态）
2. 序列化 payload 为 JSON 字符串，调用对应链码函数提交交易
3. 链码写入账本并发布事件
4. 后端监听链码事件，通过 `ChainReceiptProcessor` 分发到各业务模块的 `ChainReceiptHandler`
5. Handler 根据上链结果（成功/失败）更新数据库最终状态

## 依赖版本

- Go 1.21+
- github.com/hyperledger/fabric-contract-api-go v1.2.2
- Hyperledger Fabric 2.x
