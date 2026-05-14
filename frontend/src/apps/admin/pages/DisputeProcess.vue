<template>
  <!-- 争议处理页（管理员） -->
  <div class="dispute-process-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader :title="`争议案件 ${detail.basicInfo.caseNo}`" :biz-no="detail.basicInfo.caseNo" :status="detail.statusInfo.status" status-type="dispute">
        <template #actions>
          <el-button v-if="detail.allowedActions.includes('PROCESS')" type="primary" @click="showProcessDialog = true">处理</el-button>
        </template>
      </DetailHeader>

      <el-row :gutter="20">
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">争议信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="案件编号"><span class="font-mono">{{ detail.basicInfo.caseNo }}</span></el-descriptions-item>
              <el-descriptions-item label="争议类型">{{ detail.basicInfo.disputeType }}</el-descriptions-item>
              <el-descriptions-item label="描述">{{ detail.basicInfo.description }}</el-descriptions-item>
              <el-descriptions-item label="处理结果">{{ detail.basicInfo.resultSummary || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">关联信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="申请人"><span class="font-mono">{{ detail.relationInfo.applicantAccountNo }}</span></el-descriptions-item>
              <el-descriptions-item label="被申请人"><span class="font-mono">{{ detail.relationInfo.respondentAccountNo }}</span></el-descriptions-item>
              <el-descriptions-item label="关联订单"><span class="font-mono">{{ detail.relationInfo.orderNo || '-' }}</span></el-descriptions-item>
              <el-descriptions-item label="关联作品"><span class="font-mono">{{ detail.relationInfo.workNo || '-' }}</span></el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <!-- 证据列表 -->
      <div class="lc-card" style="margin-top: 20px;" v-if="detail.basicInfo.evidences.length">
        <div class="lc-card__title">证据材料</div>
        <el-table :data="detail.basicInfo.evidences" size="small" stripe>
          <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
          <el-table-column prop="evidenceUrl" label="附件" min-width="200">
            <template #default="{ row }"><a :href="row.evidenceUrl" target="_blank" rel="noopener noreferrer">查看</a></template>
          </el-table-column>
          <el-table-column prop="uploaderAccountNo" label="上传人" width="160" />
          <el-table-column prop="uploadTime" label="上传时间" width="170">
            <template #default="{ row }">{{ formatTime(row.uploadTime) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 处理记录 -->
      <div class="lc-card" style="margin-top: 20px;" v-if="detail.basicInfo.processRecords.length">
        <div class="lc-card__title">处理记录</div>
        <el-timeline>
          <el-timeline-item v-for="(r, i) in detail.basicInfo.processRecords" :key="i" :timestamp="formatTime(r.processTime)">
            <p><strong>{{ r.action }}</strong> — {{ r.operatorAccountNo }}</p>
            <p v-if="r.comment">{{ r.comment }}</p>
          </el-timeline-item>
        </el-timeline>
      </div>

      <div style="margin-top: 20px;"><ChainInfoCard :chain-info="detail.chainInfo" /></div>
    </template>

    <!-- 处理对话框 -->
    <el-dialog v-model="showProcessDialog" title="处理争议" width="520px">
      <el-form :model="processForm" label-width="80px">
        <el-form-item label="操作" required>
          <el-select v-model="processForm.action" style="width: 100%">
            <el-option label="受理" value="ACCEPT" />
            <el-option label="要求补证" value="EVIDENCE_PENDING" />
            <el-option label="解决" value="RESOLVE" />
            <el-option label="驳回" value="REJECT" />
            <el-option label="关闭" value="CLOSE" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input v-model="processForm.reviewComment" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="结果摘要" v-if="processForm.action === 'RESOLVE'">
          <el-input v-model="processForm.resultSummary" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProcessDialog = false">取消</el-button>
        <el-button type="primary" :loading="processing" @click="handleProcess">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { regulatorApi } from '@/shared/api'
import { DetailHeader, ChainInfoCard } from '@/shared/components'
import { formatTime, generateRequestId } from '@/shared/utils'
import type { DisputeCaseVO } from '@/shared/types'

const props = defineProps<{ caseNo: string }>()
const router = useRouter()
const loading = ref(false)
const processing = ref(false)
const detail = ref<DisputeCaseVO | null>(null)
const showProcessDialog = ref(false)
const processForm = reactive({ action: 'ACCEPT', reviewComment: '', resultSummary: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await regulatorApi.getDisputeDetail(props.caseNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleProcess() {
  processing.value = true
  try {
    await regulatorApi.processDispute({
      caseNo: props.caseNo,
      action: processForm.action,
      comment: processForm.reviewComment || undefined,
      resultSummary: processForm.resultSummary || undefined,
      requestId: generateRequestId()
    })
    ElMessage.success('处理完成')
    showProcessDialog.value = false
    loadData()
  } finally {
    processing.value = false
  }
}

onMounted(loadData)
</script>
