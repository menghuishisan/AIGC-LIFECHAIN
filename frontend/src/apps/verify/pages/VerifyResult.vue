<template>
  <!-- 验真结果页 -->
  <div class="verify-result-page" v-loading="loading">
    <template v-if="result">
      <!-- 验真状态区 -->
      <div class="result-status-card verified">
        <el-icon size="48" color="#67C23A"><CircleCheckFilled /></el-icon>
        <h2>已验证</h2>
        <p>该作品版权信息已链上确认</p>
      </div>

      <!-- 摘要卡 -->
      <div class="result-card">
        <h3>版权摘要</h3>
        <el-descriptions :column="2" border size="default">
          <el-descriptions-item label="证书编号"><span class="font-mono">{{ result.certNo }}</span></el-descriptions-item>
          <el-descriptions-item label="作品编号"><span class="font-mono">{{ result.workNo }}</span></el-descriptions-item>
          <el-descriptions-item label="证书状态">
            <el-tag :type="result.certStatus === 'CERT_ACTIVE' ? 'success' : 'info'" size="small">{{ result.certStatus }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="确权时间">{{ formatTime(result.claimTime) }}</el-descriptions-item>
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

      <!-- 链上锚定摘要 -->
      <div class="result-card">
        <h3>链上锚定摘要</h3>
        <el-descriptions :column="2" border size="default">
          <el-descriptions-item label="链上交易哈希">
            <span class="font-mono">{{ result.chainTxHash || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="区块高度">{{ result.blockHeight || '-' }}</el-descriptions-item>
          <el-descriptions-item label="链上状态">
            <el-tag v-if="result.chainTxHash" type="success" size="small">已确认</el-tag>
            <el-tag v-else type="info" size="small">待确认</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="result-actions">
        <el-button type="primary" @click="$router.push('/verify')">重新查询</el-button>
        <el-button @click="$router.push('/verify/help')">了解更多</el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { workApi } from '@/shared/api'
import { formatTime } from '@/shared/utils'
import type { VerifyResultVO } from '@/shared/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const result = ref<VerifyResultVO | null>(null)

function desensitizeDid(did?: string): string {
  if (!did) return '-'
  if (did.length <= 12) return did
  return did.slice(0, 8) + '****' + did.slice(-4)
}

async function loadResult() {
  const queryType = route.query.queryType as string
  const queryValue = route.query.queryValue as string
  if (!queryType || !queryValue) {
    router.replace('/verify')
    return
  }
  loading.value = true
  try {
    const res = await workApi.publicVerify({ queryType, queryValue })
    if (res.data?.verified) {
      result.value = res.data
    } else {
      router.replace({ path: '/verify/not-found', query: route.query })
    }
  } catch {
    router.replace({ path: '/verify/not-found', query: route.query })
  } finally {
    loading.value = false
  }
}

onMounted(loadResult)
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

  &.verified {
    background: linear-gradient(135deg, #f0f9eb, #e1f3d8);
    border: 1px solid #c2e7b0;
  }

  h2 {
    font-size: 24px;
    color: #67C23A;
    margin: 12px 0 4px;
  }

  p {
    color: #909399;
    font-size: 14px;
  }
}

.result-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;

  h3 {
    font-size: 16px;
    margin-bottom: 16px;
    color: #333;
    padding-bottom: 8px;
    border-bottom: 1px solid #eee;
  }
}

.result-actions {
  text-align: center;
  margin-top: 32px;
}
</style>
