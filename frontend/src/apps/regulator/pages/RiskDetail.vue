<template>
  <!-- 风险事件详情页 -->
  <div class="risk-detail-page" v-loading="loading">
    <DetailHeader title="风险事件详情" :bizNo="detail?.riskNo" :allowBack="true">
      <template #status>
        <StatusTag v-if="detail" :status="detail.status" type="risk" />
      </template>
      <template #actions>
        <el-button
          v-if="detail?.status === 'RISK_MARKED'"
          type="primary"
          @click="showHandleDialog = true"
        >处理</el-button>
      </template>
    </DetailHeader>

    <template v-if="detail">
      <!-- 基本信息 -->
      <div class="lc-card">
        <div class="lc-card__title">基本信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="风险编号"><span class="font-mono">{{ detail.riskNo }}</span></el-descriptions-item>
          <el-descriptions-item label="目标类型">{{ TargetTypeMap[detail.targetType] || detail.targetType }}</el-descriptions-item>
          <el-descriptions-item label="目标编号"><span class="font-mono">{{ detail.targetNo }}</span></el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="riskLevelType(detail.riskLevel)" size="small">{{ detail.riskLevel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="风险类型">{{ detail.riskType }}</el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :status="detail.status" type="risk" /></el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ detail.resolveTime ? formatTime(detail.resolveTime) : '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 风险描述 -->
      <div class="lc-card">
        <div class="lc-card__title">风险描述</div>
        <p style="white-space: pre-wrap; line-height: 1.6;">{{ detail.riskDescription }}</p>
      </div>

      <!-- 处理结论 -->
      <div class="lc-card" v-if="detail.resultSummary">
        <div class="lc-card__title">处理结论</div>
        <p style="white-space: pre-wrap; line-height: 1.6;">{{ detail.resultSummary }}</p>
      </div>
    </template>

    <!-- 处理对话框 -->
    <el-dialog v-model="showHandleDialog" title="处理风险事件" width="480px">
      <el-form :model="handleForm" label-width="90px">
        <el-form-item label="处理动作" required>
          <el-select v-model="handleForm.action" style="width: 100%">
            <el-option label="确认风险" value="CONFIRM" />
            <el-option label="释放/误报" value="RELEASE" />
            <el-option label="冻结关联" value="FREEZE" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因编码">
          <el-input v-model="handleForm.reasonCode" placeholder="可选" />
        </el-form-item>
        <el-form-item label="处理结论" required>
          <el-input v-model="handleForm.resultSummary" type="textarea" :rows="3" placeholder="请输入处理结论" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHandleDialog = false">取消</el-button>
        <el-button type="primary" :loading="handleLoading" @click="submitHandle">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { regulatorApi } from '@/shared/api'
import { TargetTypeMap } from '@/shared/constants'
import { formatTime, generateRequestId } from '@/shared/utils'
import StatusTag from '@/shared/components/StatusTag.vue'
import DetailHeader from '@/shared/components/DetailHeader.vue'
import type { RiskEventVO } from '@/shared/types'

const route = useRoute()
const riskNo = route.params.riskNo as string

const loading = ref(false)
const detail = ref<RiskEventVO | null>(null)

/** 风险等级标签颜色 */
function riskLevelType(level: string): 'success' | 'warning' | 'danger' | 'info' | undefined {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = { LOW: 'info', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }
  return map[level]
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await regulatorApi.getRiskDetail(riskNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

/* ========== 处理表单 ========== */
const showHandleDialog = ref(false)
const handleLoading = ref(false)
const handleForm = reactive({
  action: 'CONFIRM',
  reasonCode: '',
  resultSummary: ''
})

async function submitHandle() {
  if (!handleForm.resultSummary) return
  handleLoading.value = true
  try {
    await regulatorApi.handleRisk({
      riskNo,
      action: handleForm.action,
      reasonCode: handleForm.reasonCode || undefined,
      resultSummary: handleForm.resultSummary,
      requestId: generateRequestId()
    })
    ElMessage.success('处理成功')
    showHandleDialog.value = false
    loadDetail()
  } finally {
    handleLoading.value = false
  }
}

onMounted(loadDetail)
</script>
