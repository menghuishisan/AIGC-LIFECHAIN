<template>
  <!-- 订单详情页 -->
  <div class="order-detail-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader
        :title="detail.basicInfo.workTitle"
        :biz-no="detail.basicInfo.orderNo"
        :status="detail.statusInfo.orderStatus"
        status-type="order"
      >
        <template #actions>
          <el-button v-if="detail.allowedActions.includes('PAY')" type="primary" @click="router.push(`/orders/${detail.basicInfo.orderNo}/pay`)">
            去支付
          </el-button>
          <el-button v-if="detail.allowedActions.includes('CANCEL')" @click="handleCancel">取消订单</el-button>
          <el-button v-if="detail.allowedActions.includes('APPLY_REFUND')" type="warning" @click="showRefundDialog = true">申请退款</el-button>
        </template>
      </DetailHeader>

      <el-row :gutter="20">
        <!-- 订单基础信息 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">订单信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="订单编号"><span class="font-mono">{{ detail.basicInfo.orderNo }}</span></el-descriptions-item>
              <el-descriptions-item label="作品名称">{{ detail.basicInfo.workTitle }}</el-descriptions-item>
              <el-descriptions-item label="授权类型">{{ LicenseTypeMap[detail.basicInfo.licenseType] || detail.basicInfo.licenseType }}</el-descriptions-item>
              <el-descriptions-item label="标价">{{ formatCurrency(detail.basicInfo.priceAmount) }}</el-descriptions-item>
              <el-descriptions-item label="实付金额">{{ formatCurrency(detail.basicInfo.payAmount) }}</el-descriptions-item>
              <el-descriptions-item label="支付渠道">{{ PayChannelMap[detail.basicInfo.payChannel] || detail.basicInfo.payChannel }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>

        <!-- 状态与时间 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">状态与时间</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="订单状态">
                <StatusTag :status="detail.statusInfo.orderStatus" type="order" />
              </el-descriptions-item>
              <el-descriptions-item label="支付状态">
                <StatusTag :status="detail.statusInfo.payStatus" type="pay" />
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatTime(detail.timeInfo.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="过期时间">{{ formatTime(detail.timeInfo.expireTime) }}</el-descriptions-item>
              <el-descriptions-item label="支付时间">{{ formatTime(detail.timeInfo.payTime) }}</el-descriptions-item>
              <el-descriptions-item label="完成时间">{{ formatTime(detail.timeInfo.completeTime) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <!-- 关联信息 -->
      <div class="lc-card" style="margin-top: 20px;">
        <div class="lc-card__title">关联信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="作品编号">
            <el-button link type="primary" @click="router.push(`/creator/works/${detail.relationInfo.workNo}`)">{{ detail.relationInfo.workNo }}</el-button>
          </el-descriptions-item>
          <el-descriptions-item label="上架编号"><span class="font-mono">{{ detail.relationInfo.listingNo }}</span></el-descriptions-item>
          <el-descriptions-item label="授权编号">
            <el-button v-if="detail.relationInfo.licenseNo" link type="primary" @click="router.push(`/buyer/licenses/${detail.relationInfo.licenseNo}`)">
              {{ detail.relationInfo.licenseNo }}
            </el-button>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="结算编号"><span class="font-mono">{{ detail.relationInfo.settleNo || '-' }}</span></el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 链上信息 -->
      <div style="margin-top: 20px;">
        <ChainInfoCard :chain-info="detail.chainInfo" />
      </div>

      <!-- 订单轨迹 -->
      <div class="lc-card" style="margin-top: 20px;">
        <TraceTimeline title="订单轨迹" :events="traces" />
      </div>
    </template>

    <!-- 退款对话框 -->
    <el-dialog v-model="showRefundDialog" title="申请退款" width="480px">
      <el-form :model="refundForm" label-width="80px">
        <el-form-item label="退款原因" required>
          <el-input v-model="refundForm.reason" type="textarea" :rows="3" placeholder="请输入退款原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRefundDialog = false">取消</el-button>
        <el-button type="primary" :loading="refunding" @click="handleRefund">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tradeApi, appApi } from '@/shared/api'
import { DetailHeader, StatusTag, ChainInfoCard, TraceTimeline } from '@/shared/components'
import { LicenseTypeMap, PayChannelMap } from '@/shared/constants'
import { formatTime, formatCurrency, generateRequestId } from '@/shared/utils'
import type { OrderDetailVO, TraceEventVO } from '@/shared/types'

const props = defineProps<{ orderNo: string }>()
const router = useRouter()
const loading = ref(false)
const detail = ref<OrderDetailVO | null>(null)
const traces = ref<TraceEventVO[]>([])
const showRefundDialog = ref(false)
const refunding = ref(false)
const refundForm = reactive({ reason: '' })

async function loadData() {
  loading.value = true
  try {
    const [detailRes, traceRes] = await Promise.all([
      tradeApi.getOrderDetail(props.orderNo),
      appApi.getOrderTraces(props.orderNo)
    ])
    detail.value = detailRes.data
    traces.value = traceRes.data || []
  } finally {
    loading.value = false
  }
}

/** 取消订单 */
async function handleCancel() {
  await ElMessageBox.confirm('确定要取消该订单吗？', '取消订单', { type: 'warning' })
  await tradeApi.cancelOrder(props.orderNo, generateRequestId())
  ElMessage.success('订单已取消')
  loadData()
}

/** 申请退款 */
async function handleRefund() {
  if (!refundForm.reason.trim()) {
    ElMessage.warning('请输入退款原因')
    return
  }
  refunding.value = true
  try {
    await tradeApi.applyRefund(props.orderNo, {
      reason: refundForm.reason,
      requestId: generateRequestId()
    })
    ElMessage.success('退款申请已提交')
    showRefundDialog.value = false
    loadData()
  } finally {
    refunding.value = false
  }
}

onMounted(loadData)
</script>
