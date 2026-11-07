<template>
  <!-- 验真结果页 -->
  <div data-page="VerifyResult" class="lc-page verify-result-page" v-loading="loading">
    <el-result
      v-if="loadError"
      icon="warning"
      title="暂时无法完成验真"
      sub-title="验真服务没有返回结果，请稍后重试。"
    >
      <template #extra>
        <el-button type="primary" :loading="loading" @click="loadResult">重试</el-button>
        <el-button @click="router.push('/verify')">重新查询</el-button>
      </template>
    </el-result>
    <template v-if="result">
      <!-- 验真状态区 -->
      <div class="result-status-card" :class="{ verified: result.certStatus === 'CERT_ACTIVE' }">
        <el-icon size="48" :color="result.certStatus === 'CERT_ACTIVE' ? '#397756' : '#a96d27'"
          ><CircleCheckFilled
        /></el-icon>
        <h1>已找到登记记录</h1>
        <p>此结果确认平台存在匹配的证书登记记录，不代表本次公开查询已交叉核验链上状态。</p>
      </div>

      <!-- 摘要卡 -->
      <div class="result-card">
        <h3>版权摘要</h3>
        <el-descriptions :column="compact ? 1 : 2" border size="default">
          <el-descriptions-item label="证书编号"
            ><span class="font-mono">{{ result.certNo }}</span></el-descriptions-item
          >
          <el-descriptions-item label="作品编号"
            ><span class="font-mono">{{ result.workNo }}</span></el-descriptions-item
          >
          <el-descriptions-item label="证书状态">
            <el-tag
              :type="result.certStatus === 'CERT_ACTIVE' ? 'success' : 'warning'"
              size="small"
            >
              {{ CertStatusMap[result.certStatus || ''] || '状态未知' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="确权时间">{{
            formatTime(result.claimTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="验真级别">
            <el-tag size="small">{{ result.verifyLevel }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 主体脱敏摘要 -->
      <div class="result-card">
        <h3>主体脱敏摘要</h3>
        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="创作者 DID">
            <span class="font-mono">{{ desensitizeDid(result.creatorDid) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="摘要哈希">
            <span class="font-mono">{{ result.summaryHash || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 公开接口只返回登记摘要，不宣称本次请求执行了链上交叉核验。 -->
      <div class="result-card">
        <h3>公开核验范围</h3>
        <p class="verification-scope">
          本页依据平台证书登记记录与查询值匹配结果。链上交易哈希与区块信息不由公开查询接口返回。
        </p>
      </div>

      <div class="result-actions">
        <el-button type="primary" @click="$router.push('/verify')">重新查询</el-button>
        <el-button @click="$router.push('/verify/help')">了解更多</el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useCompactViewport } from '@/shared/composables/useCompactViewport'
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { workApi } from '@/shared/api'
import { formatTime } from '@/shared/utils'
import { CertStatusMap } from '@/shared/constants'
import type { VerifyResultVO } from '@/shared/types'
const compact = useCompactViewport()

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref(false)
const result = ref<VerifyResultVO | null>(null)
let activeRequest = 0

function desensitizeDid(did?: string): string {
  if (!did) return '-'
  if (did.length <= 12) return did
  return did.slice(0, 8) + '****' + did.slice(-4)
}

async function loadResult() {
  const requestId = ++activeRequest
  const queryType = route.query.queryType as string
  const queryValue = route.query.queryValue as string
  if (!queryType || !queryValue) {
    router.replace('/verify')
    return
  }
  loading.value = true
  loadError.value = false
  result.value = null
  try {
    const res = await workApi.publicVerify({ queryType, queryValue })
    if (requestId !== activeRequest) return
    if (res.data?.verified) {
      result.value = res.data
    } else {
      router.replace({ path: '/verify/not-found', query: route.query })
    }
  } catch {
    if (requestId === activeRequest) loadError.value = true
  } finally {
    if (requestId === activeRequest) loading.value = false
  }
}

watch(() => [route.query.queryType, route.query.queryValue], loadResult, { immediate: true })
</script>

<style lang="scss" scoped>
.verify-result-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}

.result-status-card {
  text-align: center;
  padding: 32px;
  margin-bottom: 24px;
  border-radius: 8px;

  border: 1px solid var(--lc-line);
  background: #fff;

  &.verified {
    background: #e9f2eb;
    border-color: #cce0d0;
  }

  h2 {
    font-size: 24px;
    color: var(--lc-accent-strong);
    margin: 12px 0 4px;
  }

  p {
    color: #55645c;
    font-size: 14px;
  }
}

.result-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  border: 1px solid var(--lc-line);
  margin-bottom: 20px;

  h3 {
    font-size: 16px;
    margin-bottom: 16px;
    color: var(--lc-ink);
    padding-bottom: 8px;
    border-bottom: 1px solid var(--lc-line);
  }
}

.verification-scope {
  color: var(--lc-muted);
  font-size: 13px;
  line-height: 1.7;
}

.result-actions {
  text-align: center;
  margin-top: 32px;
}
</style>
