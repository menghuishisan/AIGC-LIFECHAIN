<template>
  <!-- 确权详情页 -->
  <div data-page="ClaimDetail" class="lc-page claim-detail-page" v-loading="loading">
    <PageLoadFeedback :message="pageError" @retry="loadData" />
    <template v-if="detail">
      <!-- 详情头部 -->
      <DetailHeader
        :title="`确权申请 ${detail.basicInfo.claimNo}`"
        :biz-no="detail.basicInfo.claimNo"
        :status="detail.statusInfo.status"
        status-type="claim"
      >
        <template #actions>
          <el-button
            v-if="detail.allowedActions.includes('GENERATE_CERT')"
            type="primary"
            @click="handleGenerateCert"
          >
            生成证书
          </el-button>
        </template>
      </DetailHeader>

      <el-row :gutter="20">
        <!-- 基础信息 -->
        <el-col :xs="24" :md="12">
          <div class="lc-card">
            <div class="lc-card__title">基础信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="确权编号">
                <span class="font-mono">{{ detail.basicInfo.claimNo }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="摘要哈希">
                <span class="font-mono">{{ detail.basicInfo.summaryHash || '-' }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>

        <!-- 状态信息 -->
        <el-col :xs="24" :md="12">
          <div class="lc-card">
            <div class="lc-card__title">状态信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="当前状态">
                <StatusTag :status="detail.statusInfo.status" type="claim" />
              </el-descriptions-item>
              <el-descriptions-item label="审核意见">{{
                detail.statusInfo.reviewComment || '-'
              }}</el-descriptions-item>
              <el-descriptions-item label="驳回原因">{{
                detail.statusInfo.rejectReason || '-'
              }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <!-- 关联信息 -->
      <div class="lc-card" style="margin-top: 20px">
        <div class="lc-card__title">关联信息</div>
        <el-descriptions :column="compact ? 1 : 2" border size="small">
          <el-descriptions-item label="关联作品">
            <el-button
              link
              type="primary"
              @click="router.push(`/creator/works/${detail.relationInfo.workNo}`)"
            >
              {{ detail.relationInfo.workNo }}
            </el-button>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 链上信息 -->
      <div style="margin-top: 20px">
        <ChainInfoCard :chain-info="detail.chainInfo" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { usePageLoad } from '@/shared/composables/usePageLoad'
import PageLoadFeedback from '@/shared/components/PageLoadFeedback.vue'
import { useCompactViewport } from '@/shared/composables/useCompactViewport'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { workApi } from '@/shared/api'
import { DetailHeader, StatusTag, ChainInfoCard } from '@/shared/components'
import { formatTime, generateRequestId } from '@/shared/utils'
import type { ClaimDetailVO } from '@/shared/types'
const { pageError, runPageLoad } = usePageLoad()

const compact = useCompactViewport()

const props = defineProps<{ claimNo: string }>()
const router = useRouter()
const loading = ref(false)
const detail = ref<ClaimDetailVO | null>(null)

async function loadData() {
  await runPageLoad(async (isCurrent) => {
    loading.value = true
    try {
      const res = await workApi.getClaimDetail(props.claimNo)
      if (!isCurrent()) return
      detail.value = res.data
    } finally {
      if (isCurrent()) loading.value = false
    }
  })
}

/** 生成证书 */
async function handleGenerateCert() {
  await workApi.generateCertificate({ claimNo: props.claimNo, requestId: generateRequestId() })
  ElMessage.success('证书生成请求已提交')
  loadData()
}

onMounted(loadData)
</script>
