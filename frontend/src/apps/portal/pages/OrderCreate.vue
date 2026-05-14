<template>
  <!-- 下单确认页 -->
  <div class="order-create-page">
    <div class="page-header">
      <h2>确认订单</h2>
    </div>

    <div class="lc-card" style="max-width: 640px;">
      <!-- 作品信息 -->
      <div class="order-info-section">
        <h3>作品信息</h3>
        <el-descriptions :column="1" border size="small" v-loading="infoLoading">
          <el-descriptions-item label="作品名称">{{ workInfo.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="作品类型">{{ WorkTypeMap[workInfo.workType] || workInfo.workType }}</el-descriptions-item>
          <el-descriptions-item label="授权类型">{{ LicenseTypeMap[workInfo.licenseType] || workInfo.licenseType }}</el-descriptions-item>
          <el-descriptions-item label="授权范围">{{ workInfo.scopeDescription || '-' }}</el-descriptions-item>
          <el-descriptions-item label="授权天数">{{ workInfo.durationDays ? `${workInfo.durationDays}天` : '永久' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 价格 -->
      <div class="order-price-section">
        <div class="price-row">
          <span>作品价格</span>
          <span class="price-value">{{ formatCurrency(workInfo.priceAmount) }}</span>
        </div>
        <div class="price-row total">
          <span>应付金额</span>
          <span class="price-value">{{ formatCurrency(workInfo.priceAmount) }}</span>
        </div>
      </div>

      <!-- 支付方式 -->
      <div class="order-pay-section">
        <h3>选择支付方式</h3>
        <el-radio-group v-model="payChannel" size="large">
          <el-radio-button value="WECHAT_PAY">微信支付</el-radio-button>
          <el-radio-button value="ALIPAY">支付宝</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 提交 -->
      <div class="order-submit-section">
        <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit" style="width: 200px">
          提交订单
        </el-button>
        <el-button size="large" @click="router.back()">返回</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { tradeApi, workApi } from '@/shared/api'
import { WorkTypeMap, LicenseTypeMap } from '@/shared/constants'
import { formatCurrency, generateRequestId } from '@/shared/utils'

const router = useRouter()
const route = useRoute()
const submitting = ref(false)
const infoLoading = ref(false)
const payChannel = ref('WECHAT_PAY')

const listingNo = route.query.listingNo as string || ''
const workNo = route.query.workNo as string || ''

const workInfo = reactive({
  title: '',
  workType: '',
  licenseType: '',
  scopeDescription: '',
  durationDays: undefined as number | undefined,
  priceAmount: 0
})

/** 加载作品/上架信息 */
async function loadInfo() {
  infoLoading.value = true
  try {
    if (listingNo) {
      const res = await tradeApi.getListingDetail(listingNo)
      const d = res.data
      workInfo.title = d.workTitle
      workInfo.licenseType = d.licenseType
      workInfo.priceAmount = d.priceAmount
      workInfo.scopeDescription = d.scopeDescription || ''
      workInfo.durationDays = d.durationDays
    }
    if (workNo) {
      const res = await workApi.getMarketWorkDetail(workNo)
      const d = res.data
      workInfo.title = d.basicInfo?.title || workInfo.title
      workInfo.workType = d.basicInfo?.workType || ''
    }
  } finally {
    infoLoading.value = false
  }
}

/** 提交订单 */
async function handleSubmit() {
  if (!listingNo) {
    ElMessage.error('缺少上架编号')
    return
  }
  submitting.value = true
  try {
    const res = await tradeApi.createOrder({
      listingNo,
      payChannel: payChannel.value,
      requestId: generateRequestId()
    })
    ElMessage.success('订单已创建')
    /* 跳转到支付页 */
    const orderNo = res.data?.basicInfo?.orderNo
    if (orderNo) {
      router.push(`/orders/${orderNo}/pay`)
    } else {
      router.push('/buyer/dashboard')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(loadInfo)
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 16px;
}
.order-info-section,
.order-pay-section {
  margin-bottom: 24px;
  h3 {
    font-size: 16px;
    font-weight: 500;
    margin: 0 0 12px;
    color: #303133;
  }
}
.order-price-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}
.price-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  color: #606266;
  &.total {
    padding-top: 12px;
    border-top: 1px solid #eee;
    font-weight: 600;
    color: #303133;
    .price-value {
      font-size: 20px;
      color: #E6524B;
    }
  }
}
.price-value {
  font-weight: 500;
}
.order-submit-section {
  display: flex;
  gap: 12px;
  padding-top: 12px;
}
</style>
