<template>
  <!-- 确权详情页 -->
  <div class="claim-detail-page" v-loading="loading">
    <template v-if="detail">
      <!-- 详情头部 -->
      <DetailHeader
        :title="`确权申请 ${detail.basicInfo.claimNo}`"
        :biz-no="detail.basicInfo.claimNo"
        :status="detail.statusInfo.status"
        status-type="claim"
      >
        <template #actions>
          <el-button v-if="detail.allowedActions.includes('GENERATE_CERT')" type="primary" @click="handleGenerateCert">
            生成证书
          </el-button>
        </template>
      </DetailHeader>

      <el-row :gutter="20">
        <!-- 基础信息 -->
        <el-col :span="12">
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
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">状态信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="当前状态">
                <StatusTag :status="detail.statusInfo.status" type="claim" />
              </el-descriptions-item>
              <el-descriptions-item label="审核意见">{{ detail.statusInfo.reviewComment || '-' }}</el-descriptions-item>
              <el-descriptions-item label="驳回原因">{{ detail.statusInfo.rejectReason || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <!-- 关联信息 -->
      <div class="lc-card" style="margin-top: 20px;">
        <div class="lc-card__title">关联信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="关联作品">
            <el-button link type="primary" @click="router.push(`/creator/works/${detail.relationInfo.workNo}`)">
              {{ detail.relationInfo.workNo }}
            </el-button>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 链上信息 -->
      <div style="margin-top: 20px;">
        <ChainInfoCard :chain-info="detail.chainInfo" />
      </div>

      <!-- 链回执 -->
      <div class="lc-card" style="margin-top: 20px;" v-if="chainReceipt">
        <div class="lc-card__title">链上回执</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="交易哈希"><span class="font-mono">{{ chainReceipt.txHash }}</span></el-descriptions-item>
          <el-descriptions-item label="区块高度">{{ chainReceipt.blockHeight || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上链状态">
            <StatusTag :status="chainReceipt.chainStatus" type="chain" />
          </el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatTime(chainReceipt.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { workApi } from '@/shared/api'
import { DetailHeader, StatusTag, ChainInfoCard } from '@/shared/components'
import { formatTime, generateRequestId } from '@/shared/utils'
import type { ClaimDetailVO, ChainReceiptVO } from '@/shared/types'

const props = defineProps<{ claimNo: string }>()
const router = useRouter()
const loading = ref(false)
const detail = ref<ClaimDetailVO | null>(null)
const chainReceipt = ref<ChainReceiptVO | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await workApi.getClaimDetail(props.claimNo)
    detail.value = res.data
    /* 尝试获取链回执（接口返回 ClaimDetailVO，取其 chainInfo） */
    try {
      const receiptRes = await workApi.getClaimChainReceipt(props.claimNo)
      const info = receiptRes.data?.chainInfo
      if (info) {
        chainReceipt.value = {
          bizType: 'CLAIM',
          bizNo: props.claimNo,
          txHash: info.txHash || '',
          blockHeight: info.blockHeight,
          chainStatus: info.chainStatus || '',
          createdAt: ''
        }
      }
    } catch { /* 可能尚无链回执 */ }
  } finally {
    loading.value = false
  }
}

/** 生成证书 */
async function handleGenerateCert() {
  await workApi.generateCertificate({ claimNo: props.claimNo, requestId: generateRequestId() })
  ElMessage.success('证书生成请求已提交')
  loadData()
}

onMounted(loadData)
</script>
