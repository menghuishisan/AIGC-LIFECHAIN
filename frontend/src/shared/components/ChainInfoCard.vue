<template>
  <!-- 链上信息卡组件 -->
  <div class="chain-info-card" v-if="chainInfo">
    <div class="chain-info-card__title">链上信息</div>
    <el-descriptions :column="2" border size="small">
      <el-descriptions-item label="交易哈希">
        <span class="font-mono">{{ chainInfo.txHash || '-' }}</span>
        <el-button
          v-if="chainInfo.txHash"
          link
          type="primary"
          size="small"
          @click="onCopy(chainInfo.txHash!)"
        >
          复制
        </el-button>
      </el-descriptions-item>
      <el-descriptions-item label="区块高度">
        <span class="font-mono">{{ chainInfo.blockHeight ?? '-' }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="链上状态">
        <StatusTag :status="chainInfo.chainStatus || ''" type="chain" />
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { ChainInfo } from '@/shared/types'
import StatusTag from './StatusTag.vue'
import { copyToClipboard } from '@/shared/utils'

defineProps<{
  chainInfo?: ChainInfo | null
}>()

/** 复制到剪贴板 */
async function onCopy(text: string) {
  const ok = await copyToClipboard(text)
  if (ok) {
    ElMessage.success('已复制')
  }
}
</script>

<style lang="scss" scoped>
.chain-info-card {
  background: #E7F4F5;
  border-radius: 12px;
  padding: 16px;

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: #0B7FA2;
    margin-bottom: 12px;
  }
}
</style>
