# Runtime Config

本目录用于放置后端运行期依赖的本地配置与运维资产，不属于源码实现的一部分。

当前约定：

- `fabric/`：Hyperledger Fabric Gateway 连接配置、证书、私钥、TLS CA

要求：

- 运行时文件只允许在 `backend/runtime-config/` 下维护，不放入 `src/main/resources` 或业务模块源码目录。
- 证书、私钥、connection profile 属于部署/联调资产，不应提交到仓库。
- 后端通过环境变量引用本目录中的文件路径。
