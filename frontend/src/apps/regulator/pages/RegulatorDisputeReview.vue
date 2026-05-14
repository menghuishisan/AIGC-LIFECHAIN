<template>
  <!-- 监管争议分析页 -->
  <div class="regulator-dispute-review-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader title="争议分析" :bizNo="detail?.basicInfo?.caseNo" :allowBack="true">
        <template #status>
          <StatusTag :status="detail.statusInfo.status" type="dispute" />
        </template>
      </DetailHeader>

      <!-- 案件概要 -->
      <div class="lc-card">
        <div class="lc-card__title">案件概要</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="案件编号"><span class="font-mono">{{ detail.basicInfo.caseNo }}</span></el-descriptions-item>
          <el-descriptions-item label="争议类型">{{ detail.basicInfo.disputeType }}</el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :status="detail.statusInfo.status" type="dispute" /></el-descriptions-item>
          <el-descriptions-item label="申请方"><span class="font-mono">{{ detail.relationInfo.applicantAccountNo }}</span></el-descriptions-item>
          <el-descriptions-item label="被申请方"><span class="font-mono">{{ detail.relationInfo.respondentAccountNo }}</span></el-descriptions-item>
          <el-descriptions-item label="关联订单">
            <span class="font-mono" v-if="detail.relationInfo.orderNo">{{ detail.relationInfo.orderNo }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 争议描述 -->
      <div class="lc-card">
        <div class="lc-card__title">争议描述</div>
        <p style="white-space: pre-wrap; line-height: 1.6;">{{ detail.basicInfo.description }}</p>
      </div>

      <!-- 处理结论 -->
      <div class="lc-card" v-if="detail.basicInfo.resultSummary">
        <div class="lc-card__title">处理结论</div>
        <p style="white-space: pre-wrap; line-height: 1.6;">{{ detail.basicInfo.resultSummary }}</p>
      </div>

      <!-- 证据材料 -->
      <div class="lc-card" v-if="detail.basicInfo.evidences?.length">
        <div class="lc-card__title">证据材料</div>
        <el-table :data="detail.basicInfo.evidences" stripe size="small">
          <el-table-column label="证据URL" prop="evidenceUrl" show-overflow-tooltip />
          <el-table-column label="说明" prop="description" width="200" show-overflow-tooltip />
          <el-table-column label="上传方" prop="uploaderAccountNo" width="200" show-overflow-tooltip />
          <el-table-column label="上传时间" width="170">
            <template #default="{ row }">{{ formatTime(row.uploadTime) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 处理记录 -->
      <div class="lc-card" v-if="detail.basicInfo.processRecords?.length">
        <div class="lc-card__title">处理记录</div>
        <el-timeline>
          <el-timeline-item
            v-for="(record, idx) in detail.basicInfo.processRecords"
            :key="idx"
            :timestamp="formatTime(record.processTime)"
            placement="top"
          >
            <p><strong>{{ record.action }}</strong> — 操作人: {{ record.operatorAccountNo }}</p>
            <p v-if="record.comment">{{ record.comment }}</p>
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- 链上信息 -->
      <ChainInfoCard v-if="detail.chainInfo" :chainInfo="detail.chainInfo" />

      <!-- 监管研判面板（仅查看与留痕，不提交正式处理结论） -->
      <div class="lc-card">
        <div class="lc-card__title">监管研判面板</div>
        <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
          此页面用于监管核查、留痕和建议动作，不承担争议结论提交。争议的正式处理结论由平台管理员完成。
        </el-alert>
        <el-form :model="reviewForm" label-width="100px" @submit.prevent>
          <el-form-item label="研判意见">
            <el-input
              v-model="reviewForm.opinion"
              type="textarea"
              :rows="3"
              placeholder="填写监管研判意见（本地留痕）"
            />
          </el-form-item>
          <el-form-item label="风险结论">
            <el-radio-group v-model="reviewForm.riskLevel">
              <el-radio value="NORMAL">正常</el-radio>
              <el-radio value="SUSPICIOUS">可疑</el-radio>
              <el-radio value="HIGH_RISK">高风险</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="建议动作">
            <el-space>
              <el-button size="small" @click="handleSuggestAction('PLATFORM_PROCESS')">建议平台处理争议</el-button>
              <el-button size="small" @click="handleSuggestAction('RISK_EVENT')">转风险事件</el-button>
              <el-button size="small" @click="handleSuggestAction('FREEZE')">发起冻结</el-button>
            </el-space>
          </el-form-item>
        </el-form>
      </div>

      <!-- 补充证据 -->
      <div class="lc-card">
        <div class="lc-card__title">补充证据</div>
        <el-form :model="evidenceForm" label-width="100px" @submit.prevent>
          <el-form-item label="证据类型" required>
            <el-select v-model="evidenceForm.evidenceType" placeholder="选择证据类型" style="width: 100%">
              <el-option label="截图" value="SCREENSHOT" />
              <el-option label="文档" value="DOCUMENT" />
              <el-option label="视频" value="VIDEO" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="文件URL" required>
            <el-input v-model="evidenceForm.fileUrl" placeholder="输入证据文件 URL" />
          </el-form-item>
          <el-form-item label="说明">
            <el-input v-model="evidenceForm.description" placeholder="可选，填写证据说明" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleAddEvidence">提交证据</el-button>
          </el-form-item>
        </el-form>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { regulatorApi } from '@/shared/api'
import { formatTime, generateRequestId } from '@/shared/utils'
import StatusTag from '@/shared/components/StatusTag.vue'
import DetailHeader from '@/shared/components/DetailHeader.vue'
import ChainInfoCard from '@/shared/components/ChainInfoCard.vue'
import type { DisputeCaseVO } from '@/shared/types'

const route = useRoute()
const router = useRouter()
const caseNo = route.params.caseNo as string

const loading = ref(false)
const submitting = ref(false)
const detail = ref<DisputeCaseVO | null>(null)

const reviewForm = reactive({
  opinion: '',
  riskLevel: '',
})

const evidenceForm = reactive({
  evidenceType: '',
  fileUrl: '',
  description: '',
})

async function loadDetail() {
  loading.value = true
  try {
    const res = await regulatorApi.getDisputeDetail(caseNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

function handleSuggestAction(action: string) {
  if (action === 'RISK_EVENT') {
    ElMessage.info('建议已记录：转风险事件')
  } else if (action === 'FREEZE') {
    router.push('/regulator/freezes')
  } else {
    ElMessage.info('建议已记录：建议平台处理争议')
  }
}

async function handleAddEvidence() {
  if (!evidenceForm.fileUrl) {
    ElMessage.warning('请输入证据文件 URL')
    return
  }
  if (!evidenceForm.evidenceType) {
    ElMessage.warning('请选择证据类型')
    return
  }
  submitting.value = true
  try {
    await regulatorApi.addEvidence(caseNo, {
      evidenceType: evidenceForm.evidenceType,
      fileUrl: evidenceForm.fileUrl,
      description: evidenceForm.description || undefined,
      requestId: generateRequestId(),
    })
    ElMessage.success('证据提交成功')
    evidenceForm.evidenceType = ''
    evidenceForm.fileUrl = ''
    evidenceForm.description = ''
    await loadDetail()
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>
