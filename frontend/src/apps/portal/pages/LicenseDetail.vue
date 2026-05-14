<template>
  <!-- 授权详情页 -->
  <div class="license-detail-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader
        :title="`授权许可 ${detail.basicInfo.licenseNo}`"
        :biz-no="detail.basicInfo.licenseNo"
        :status="detail.statusInfo.licenseStatus"
        status-type="license"
      />

      <el-row :gutter="20">
        <!-- 基础信息 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">授权信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="授权编号"><span class="font-mono">{{ detail.basicInfo.licenseNo }}</span></el-descriptions-item>
              <el-descriptions-item label="授权类型">{{ LicenseTypeMap[detail.basicInfo.licenseType] || detail.basicInfo.licenseType }}</el-descriptions-item>
              <el-descriptions-item label="授权范围">{{ detail.basicInfo.scopeDescription || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>

        <!-- 状态与时间 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">状态与有效期</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="当前状态">
                <StatusTag :status="detail.statusInfo.licenseStatus" type="license" />
              </el-descriptions-item>
              <el-descriptions-item label="生效时间">{{ formatTime(detail.timeInfo.effectiveTime) }}</el-descriptions-item>
              <el-descriptions-item label="过期时间">{{ formatTime(detail.timeInfo.expireTime) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <!-- 关联信息 -->
      <div class="lc-card" style="margin-top: 20px;">
        <div class="lc-card__title">关联信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="关联订单">
            <el-button link type="primary" @click="router.push(`/buyer/orders/${detail.relationInfo.orderNo}`)">
              {{ detail.relationInfo.orderNo }}
            </el-button>
          </el-descriptions-item>
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
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tradeApi } from '@/shared/api'
import { DetailHeader, StatusTag, ChainInfoCard } from '@/shared/components'
import { LicenseTypeMap } from '@/shared/constants'
import { formatTime } from '@/shared/utils'
import type { LicenseDetailVO } from '@/shared/types'

const props = defineProps<{ licenseNo: string }>()
const router = useRouter()
const loading = ref(false)
const detail = ref<LicenseDetailVO | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await tradeApi.getLicenseDetail(props.licenseNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
