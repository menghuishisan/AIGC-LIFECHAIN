<template>
  <!-- 监管验真查询页 -->
  <div class="regulator-verify-page">
    <div class="page-header"><h2>监管验真</h2></div>

    <el-tabs v-model="activeTab">
      <!-- 单条验真 -->
      <el-tab-pane label="单条验真" name="single">
        <div class="lc-card" style="max-width: 640px;">
          <el-form :model="form" label-width="100px">
            <el-form-item label="查询类型" required>
              <el-select v-model="form.queryType" style="width: 100%">
                <el-option label="证书编号" value="CERT_NO" />
                <el-option label="作品编号" value="WORK_NO" />
                <el-option label="作品哈希" value="WORK_HASH" />
                <el-option label="交易哈希" value="TX_HASH" />
              </el-select>
            </el-form-item>
            <el-form-item label="查询值" required>
              <el-input v-model="form.queryValue" placeholder="请输入查询值" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleVerify">验真查询</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 验真结果 -->
        <div class="lc-card" style="margin-top: 20px; max-width: 640px;" v-if="result">
          <div class="lc-card__title">验真结果</div>
          <el-result :icon="result.verified ? 'success' : 'error'" :title="result.verified ? '验真通过' : '验真未通过'">
            <template #sub-title>
              <span v-if="result.verified">作品版权信息已上链确认</span>
              <span v-else>未找到匹配的版权记录</span>
            </template>
          </el-result>

          <el-descriptions :column="1" border size="small" v-if="result.verified" style="margin-top: 16px;">
            <el-descriptions-item label="证书编号"><span class="font-mono">{{ result.certNo }}</span></el-descriptions-item>
            <el-descriptions-item label="作品编号"><span class="font-mono">{{ result.workNo }}</span></el-descriptions-item>
            <el-descriptions-item label="创作者 DID"><span class="font-mono">{{ result.creatorDid }}</span></el-descriptions-item>
            <el-descriptions-item label="确权时间">{{ formatTime(result.claimTime) }}</el-descriptions-item>
            <el-descriptions-item label="证书状态">{{ result.certStatus }}</el-descriptions-item>
            <el-descriptions-item label="链上哈希"><span class="font-mono">{{ result.chainTxHash }}</span></el-descriptions-item>
            <el-descriptions-item label="区块高度">{{ result.blockHeight || '-' }}</el-descriptions-item>
            <el-descriptions-item label="验真级别">{{ result.verifyLevel }}</el-descriptions-item>
            <el-descriptions-item label="摘要哈希"><span class="font-mono">{{ result.summaryHash }}</span></el-descriptions-item>
          </el-descriptions>
        </div>
      </el-tab-pane>

      <!-- 批量验真 -->
      <el-tab-pane label="批量验真" name="batch">
        <div class="lc-card" style="max-width: 640px;">
          <el-form label-width="100px">
            <el-form-item label="查询类型">
              <el-select v-model="batchQueryType" style="width: 100%">
                <el-option label="证书编号" value="CERT_NO" />
                <el-option label="作品编号" value="WORK_NO" />
                <el-option label="作品哈希" value="WORK_HASH" />
                <el-option label="交易哈希" value="TX_HASH" />
              </el-select>
            </el-form-item>
            <el-form-item label="批量值">
              <el-input v-model="batchValues" type="textarea" :rows="6" placeholder="每行输入一个查询值" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="batchLoading" @click="handleBatchVerify">批量验真</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 批量结果 -->
        <div class="lc-card" style="margin-top: 20px;" v-if="batchResults.length">
          <div class="lc-card__title">批量验真结果</div>
          <el-table :data="batchResults" stripe size="small">
            <el-table-column prop="queryValue" label="查询值" min-width="180">
              <template #default="{ row }"><span class="font-mono">{{ row.queryValue }}</span></template>
            </el-table-column>
            <el-table-column prop="verified" label="结果" width="100">
              <template #default="{ row }">
                <el-tag :type="row.verified ? 'success' : 'danger'" size="small">{{ row.verified ? '通过' : '未通过' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="certNo" label="证书编号" min-width="160">
              <template #default="{ row }"><span class="font-mono">{{ row.certNo || '-' }}</span></template>
            </el-table-column>
            <el-table-column prop="workNo" label="作品编号" min-width="160">
              <template #default="{ row }"><span class="font-mono">{{ row.workNo || '-' }}</span></template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { workApi } from '@/shared/api'
import { formatTime, generateRequestId } from '@/shared/utils'
import type { VerifyResultVO } from '@/shared/types'

const activeTab = ref('single')
const loading = ref(false)
const result = ref<VerifyResultVO | null>(null)
const form = reactive({ queryType: 'CERT_NO', queryValue: '' })

async function handleVerify() {
  if (!form.queryValue.trim()) return
  loading.value = true
  result.value = null
  try {
    const res = await workApi.regulatorVerify({ queryType: form.queryType, queryValue: form.queryValue })
    result.value = res.data
  } finally {
    loading.value = false
  }
}

/* ========== 批量验真 ========== */
const batchQueryType = ref('CERT_NO')
const batchValues = ref('')
const batchLoading = ref(false)
const batchResults = ref<(VerifyResultVO & { queryValue: string })[]>([])

async function handleBatchVerify() {
  const lines = batchValues.value.split('\n').map(l => l.trim()).filter(Boolean)
  if (!lines.length) {
    ElMessage.warning('请输入至少一个查询值')
    return
  }
  batchLoading.value = true
  batchResults.value = []
  try {
    const requests = lines.map(v => ({ queryType: batchQueryType.value, queryValue: v }))
    const res = await workApi.batchVerify(requests, generateRequestId())
    batchResults.value = (res.data || []).map((r: VerifyResultVO, i: number) => ({ ...r, queryValue: lines[i] || '' }))
    ElMessage.success(`已完成 ${batchResults.value.length} 条验真`)
  } finally {
    batchLoading.value = false
  }
}
</script>
