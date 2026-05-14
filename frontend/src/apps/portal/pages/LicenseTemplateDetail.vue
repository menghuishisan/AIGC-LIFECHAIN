<template>
  <!-- 授权模板详情页 -->
  <div class="license-template-detail-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader :title="detail.templateName" :biz-no="detail.templateCode" :status="detail.status" status-type="license" />

      <div class="lc-card">
        <div class="lc-card__title">模板信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="模板编号"><span class="font-mono">{{ detail.templateCode }}</span></el-descriptions-item>
          <el-descriptions-item label="模板名称">{{ detail.templateName }}</el-descriptions-item>
          <el-descriptions-item label="授权类型">{{ LicenseTypeMap[detail.licenseType] || detail.licenseType }}</el-descriptions-item>
          <el-descriptions-item label="建议价格">{{ detail.priceAmount ? formatCurrency(detail.priceAmount) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="授权天数">{{ detail.durationDays ? `${detail.durationDays}天` : '永久' }}</el-descriptions-item>
          <el-descriptions-item label="货币">{{ detail.currency || 'CNY' }}</el-descriptions-item>
          <el-descriptions-item label="授权范围" :span="2">{{ detail.scopeDescription || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { tradeApi } from '@/shared/api'
import { DetailHeader } from '@/shared/components'
import { LicenseTypeMap } from '@/shared/constants'
import { formatTime, formatCurrency } from '@/shared/utils'
import type { LicenseTemplateVO } from '@/shared/types'

const props = defineProps<{ templateCode: string }>()
const loading = ref(false)
const detail = ref<LicenseTemplateVO | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await tradeApi.getLicenseTemplateDetail(props.templateCode)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
