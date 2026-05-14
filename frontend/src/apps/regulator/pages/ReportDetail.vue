<template>
  <!-- 监管报告详情页 -->
  <div class="report-detail-page" v-loading="loading">
    <DetailHeader title="监管报告详情" :bizNo="detail?.reportNo" :allowBack="true">
      <template #status>
        <el-tag v-if="detail" :type="reportStatusType(detail.status)" size="small">{{ reportStatusLabel(detail.status) }}</el-tag>
      </template>
      <template #actions>
        <el-button
          v-if="detail?.status === 'DRAFT'"
          type="primary"
          @click="openHandleDialog('GENERATE')"
        >触发生成</el-button>
        <el-button
          v-if="detail?.status === 'GENERATING'"
          type="success"
          @click="openHandleDialog('COMPLETE')"
        >完成报告</el-button>
        <el-button
          v-if="detail?.status === 'GENERATING'"
          type="danger"
          @click="openHandleDialog('FAIL')"
        >标记失败</el-button>
      </template>
    </DetailHeader>

    <template v-if="detail">
      <!-- 基本信息 -->
      <div class="lc-card">
        <div class="lc-card__title">报告信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="报告编号"><span class="font-mono">{{ detail.reportNo }}</span></el-descriptions-item>
          <el-descriptions-item label="报告类型">{{ detail.reportType }}</el-descriptions-item>
          <el-descriptions-item label="报告标题" :span="2">{{ detail.reportTitle }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ detail.generateTime ? formatTime(detail.generateTime) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 报告内容 -->
      <div class="lc-card">
        <div class="lc-card__title">报告内容</div>
        <div style="white-space: pre-wrap; line-height: 1.8; max-height: 500px; overflow-y: auto;">
          {{ detail.reportContent }}
        </div>
      </div>

      <!-- 报告文件 -->
      <div class="lc-card" v-if="detail.reportFileUrl">
        <div class="lc-card__title">报告文件</div>
        <el-link type="primary" :href="detail.reportFileUrl" target="_blank">下载报告文件</el-link>
      </div>

      <!-- 链上信息 -->
      <div class="lc-card" v-if="detail.txHash">
        <div class="lc-card__title">链上信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="链上状态">{{ ChainStatusMap[detail.chainStatus!] || detail.chainStatus }}</el-descriptions-item>
          <el-descriptions-item label="摘要哈希"><span class="font-mono">{{ detail.summaryHash || '-' }}</span></el-descriptions-item>
          <el-descriptions-item label="交易哈希"><span class="font-mono">{{ detail.txHash }}</span></el-descriptions-item>
          <el-descriptions-item label="区块高度">{{ detail.blockHeight || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 处理记录 -->
      <div class="lc-card">
        <div class="lc-card__title">处理记录</div>
        <el-timeline>
          <el-timeline-item timestamp="" placement="top">
            <div class="timeline-label">创建报告</div>
            <div class="timeline-time">{{ formatTime(detail.createdAt) }}</div>
          </el-timeline-item>
          <el-timeline-item v-if="detail.status !== 'DRAFT'" timestamp="" placement="top">
            <div class="timeline-label">触发生成</div>
            <div class="timeline-time">{{ detail.generateTime ? formatTime(detail.generateTime) : '-' }}</div>
          </el-timeline-item>
          <el-timeline-item v-if="detail.status === 'COMPLETED'" type="success" timestamp="" placement="top">
            <div class="timeline-label">报告完成</div>
            <div class="timeline-time">{{ detail.generateTime ? formatTime(detail.generateTime) : '-' }}</div>
          </el-timeline-item>
          <el-timeline-item v-if="detail.status === 'FAILED'" type="danger" timestamp="" placement="top">
            <div class="timeline-label">生成失败</div>
          </el-timeline-item>
          <el-timeline-item v-if="detail.txHash" type="success" timestamp="" placement="top">
            <div class="timeline-label">链上存证</div>
            <div class="timeline-time">交易哈希：{{ detail.txHash }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </template>

    <!-- 处理对话框 -->
    <el-dialog v-model="showHandleDialog" :title="handleDialogTitle" width="420px">
      <p>{{ handleDialogDesc }}</p>
      <template #footer>
        <el-button @click="showHandleDialog = false">取消</el-button>
        <el-button type="primary" :loading="handleLoading" @click="submitHandle">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { regulatorApi } from '@/shared/api'
import { ChainStatusMap } from '@/shared/constants'
import { formatTime, generateRequestId } from '@/shared/utils'
import DetailHeader from '@/shared/components/DetailHeader.vue'
import type { ReportVO } from '@/shared/types'

const route = useRoute()
const reportNo = route.params.reportNo as string

const loading = ref(false)
const detail = ref<ReportVO | null>(null)

/** 报告状态标签映射 */
function reportStatusLabel(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿', GENERATING: '生成中', COMPLETED: '已完成', FAILED: '失败'
  }
  return map[status] || status
}

function reportStatusType(status: string): 'success' | 'warning' | 'danger' | 'info' | undefined {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    DRAFT: 'info', GENERATING: 'warning', COMPLETED: 'success', FAILED: 'danger'
  }
  return map[status]
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await regulatorApi.getReportDetail(reportNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

/* ========== 处理表单 ========== */
const showHandleDialog = ref(false)
const handleLoading = ref(false)
const handleForm = reactive({ action: 'GENERATE' })

const actionLabelMap: Record<string, string> = {
  GENERATE: '触发生成',
  COMPLETE: '完成报告',
  FAIL: '标记失败'
}
const actionDescMap: Record<string, string> = {
  GENERATE: '将报告从草稿状态推进到生成中，后台将开始处理。',
  COMPLETE: '确认报告已完成，系统将生成报告文件并提交链上存证。',
  FAIL: '将当前报告标记为生成失败。'
}
const handleDialogTitle = computed(() => actionLabelMap[handleForm.action] || '处理报告')
const handleDialogDesc = computed(() => actionDescMap[handleForm.action] || '')

function openHandleDialog(action: string) {
  handleForm.action = action
  showHandleDialog.value = true
}

async function submitHandle() {
  handleLoading.value = true
  try {
    await regulatorApi.handleReport(reportNo, {
      action: handleForm.action,
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
