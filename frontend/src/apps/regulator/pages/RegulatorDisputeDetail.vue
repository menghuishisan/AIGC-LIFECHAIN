<template>
  <!-- 监管争议详情页 -->
  <div class="regulator-dispute-detail-page" v-loading="loading">
    <DetailHeader title="争议详情" :bizNo="detail?.basicInfo?.caseNo" :allowBack="true">
      <template #status>
        <StatusTag v-if="detail" :status="detail.statusInfo.status" type="dispute" />
      </template>
      <!-- 监管方仅查看，不可处理争议 -->
    </DetailHeader>

    <template v-if="detail">
      <!-- 基本信息 -->
      <div class="lc-card">
        <div class="lc-card__title">案件信息</div>
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
          <el-descriptions-item label="关联作品">
            <span class="font-mono" v-if="detail.relationInfo.workNo">{{ detail.relationInfo.workNo }}</span>
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

      <!-- 证据列表 -->
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

      <!-- 时间信息 -->
      <div class="lc-card" v-if="Object.keys(detail.timeInfo || {}).length">
        <div class="lc-card__title">时间节点</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item v-for="(val, key) in detail.timeInfo" :key="key" :label="String(key)">
            {{ formatTime(val) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 链上信息 -->
      <ChainInfoCard v-if="detail.chainInfo" :chainInfo="detail.chainInfo" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { regulatorApi } from '@/shared/api'
import { formatTime } from '@/shared/utils'
import StatusTag from '@/shared/components/StatusTag.vue'
import DetailHeader from '@/shared/components/DetailHeader.vue'
import ChainInfoCard from '@/shared/components/ChainInfoCard.vue'
import type { DisputeCaseVO } from '@/shared/types'

const route = useRoute()
const caseNo = route.params.caseNo as string

const loading = ref(false)
const detail = ref<DisputeCaseVO | null>(null)

async function loadDetail() {
  loading.value = true
  try {
    const res = await regulatorApi.getDisputeDetail(caseNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>
