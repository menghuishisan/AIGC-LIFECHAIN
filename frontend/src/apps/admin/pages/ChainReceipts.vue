<template>
  <!-- 链上回执管理页（管理员） -->
  <div class="chain-receipts-page">
    <div class="page-header"><h2>链上回执</h2></div>

    <div class="filter-bar">
      <el-select v-model="query.bizType" placeholder="业务类型" style="width: 150px">
        <el-option v-for="(label, value) in BizTypeMap" :key="value" :label="label" :value="value" />
      </el-select>
      <el-input v-model="query.bizNo" placeholder="业务编号" style="width: 200px" @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <div class="lc-card" v-if="receipt" v-loading="loading">
      <div class="lc-card__title">回执信息</div>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="业务类型">{{ BizTypeMap[receipt.bizType] || receipt.bizType }}</el-descriptions-item>
        <el-descriptions-item label="业务编号"><span class="font-mono">{{ receipt.bizNo }}</span></el-descriptions-item>
        <el-descriptions-item label="交易哈希"><span class="font-mono">{{ receipt.txHash }}</span></el-descriptions-item>
        <el-descriptions-item label="区块高度">{{ receipt.blockHeight || '-' }}</el-descriptions-item>
        <el-descriptions-item label="链上状态"><StatusTag :status="receipt.chainStatus" type="chain" /></el-descriptions-item>
        <el-descriptions-item label="失败原因">{{ receipt.failReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="通道">{{ receipt.channelName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="链码">{{ receipt.chaincodeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="背书摘要" :span="2">{{ receipt.endorsementSummary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatTime(receipt.createdAt) }}</el-descriptions-item>
      </el-descriptions>

      <!-- 操作按钮 -->
      <div style="margin-top: 16px; display: flex; gap: 12px;" v-if="receipt.chainStatus === 'CHAIN_FAILED' || receipt.txHash">
        <el-button v-if="receipt.txHash" @click="viewTxDetail">查看交易详情</el-button>
        <el-button v-if="receipt.chainStatus === 'CHAIN_FAILED'" type="primary" :loading="retrying" @click="handleRetry">重试上链</el-button>
      </div>
    </div>

    <el-empty v-if="searched && !receipt && !loading" description="未找到链上回执" />

    <!-- 交易详情对话框 -->
    <el-dialog v-model="showTxDetailDialog" title="链交易详情" width="640px">
      <el-descriptions v-if="txDetail" :column="2" border size="small">
        <el-descriptions-item label="业务类型">{{ BizTypeMap[txDetail.bizType] || txDetail.bizType }}</el-descriptions-item>
        <el-descriptions-item label="业务编号"><span class="font-mono">{{ txDetail.bizNo }}</span></el-descriptions-item>
        <el-descriptions-item label="交易类型">{{ txDetail.txType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="链上状态"><StatusTag :status="txDetail.chainStatus" type="chain" /></el-descriptions-item>
        <el-descriptions-item label="交易哈希" :span="2"><span class="font-mono">{{ txDetail.txHash }}</span></el-descriptions-item>
        <el-descriptions-item label="区块高度">{{ txDetail.blockHeight || '-' }}</el-descriptions-item>
        <el-descriptions-item label="通道">{{ txDetail.channelName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="链码">{{ txDetail.chaincodeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="失败原因">{{ txDetail.failReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="背书摘要" :span="2">{{ txDetail.endorsementSummary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatTime(txDetail.submitTime) }}</el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ txDetail.confirmTime ? formatTime(txDetail.confirmTime) : '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { appApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { BizTypeMap } from '@/shared/constants'
import { formatTime, generateRequestId } from '@/shared/utils'
import type { ChainReceiptVO } from '@/shared/types'

const loading = ref(false)
const retrying = ref(false)
const searched = ref(false)
const receipt = ref<ChainReceiptVO | null>(null)
const txDetail = ref<any>(null)
const showTxDetailDialog = ref(false)
const query = reactive({ bizType: '', bizNo: '' })

async function handleSearch() {
  if (!query.bizType || !query.bizNo) {
    ElMessage.warning('请输入业务类型和业务编号')
    return
  }
  loading.value = true
  searched.value = true
  try {
    const res = await appApi.getChainReceipt(query.bizType, query.bizNo)
    const list = res.data
    receipt.value = Array.isArray(list) ? list[0] || null : list
  } catch {
    receipt.value = null
  } finally {
    loading.value = false
  }
}

async function handleRetry() {
  if (!receipt.value?.txHash) return
  retrying.value = true
  try {
    await appApi.retryChainTx({ txHash: receipt.value.txHash, requestId: generateRequestId() })
    ElMessage.success('重试已发起')
    handleSearch()
  } finally {
    retrying.value = false
  }
}

async function viewTxDetail() {
  if (!receipt.value?.txHash) return
  try {
    const res = await appApi.getChainTxDetail(receipt.value.txHash)
    txDetail.value = res.data
    showTxDetailDialog.value = true
  } catch {
    ElMessage.error('获取交易详情失败')
  }
}
</script>

<style scoped lang="scss">
.page-header { margin-bottom: 16px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
</style>
