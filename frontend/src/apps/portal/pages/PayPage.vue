<template>
  <!-- 支付页面 -->
  <div class="pay-page" v-loading="loading">
    <div class="page-header">
      <h2>订单支付</h2>
    </div>

    <div class="lc-card pay-card" style="max-width: 560px; margin: 0 auto;">
      <!-- 订单信息 -->
      <div class="pay-order-info" v-if="order">
        <div class="pay-amount">
          <span class="pay-amount__label">支付金额</span>
          <span class="pay-amount__value">{{ formatCurrency(order.basicInfo.payAmount) }}</span>
        </div>
        <el-descriptions :column="1" size="small" style="margin-top: 16px;">
          <el-descriptions-item label="订单编号"><span class="font-mono">{{ order.basicInfo.orderNo }}</span></el-descriptions-item>
          <el-descriptions-item label="作品名称">{{ order.basicInfo.workTitle }}</el-descriptions-item>
          <el-descriptions-item label="支付渠道">{{ PayChannelMap[order.basicInfo.payChannel] || order.basicInfo.payChannel }}</el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ formatTime(order.timeInfo.expireTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 支付操作 -->
      <div class="pay-actions" v-if="order">
        <!-- 若已支付 -->
        <template v-if="['PAY_CONFIRMED', 'AUTH_GRANTING', 'AUTH_GRANTED', 'ORDER_COMPLETED'].includes(order.statusInfo.orderStatus)">
          <el-result icon="success" title="支付成功" sub-title="您的订单已支付成功">
            <template #extra>
              <el-button type="primary" @click="router.push(`/buyer/orders/${orderNo}`)">查看订单</el-button>
            </template>
          </el-result>
        </template>

        <!-- 若已取消/过期 -->
        <template v-else-if="['ORDER_CANCELLED', 'ORDER_EXPIRED'].includes(order.statusInfo.orderStatus)">
          <el-result icon="warning" title="订单已失效" sub-title="该订单已取消或过期">
            <template #extra>
              <el-button @click="router.push('/market')">返回市场</el-button>
            </template>
          </el-result>
        </template>

        <!-- 待支付 -->
        <template v-else>
          <div class="pay-qr-area">
            <!-- 已发起支付，显示支付信息 -->
            <template v-if="payResult">
              <p class="pay-channel-tip">请使用{{ PayChannelMap[order.basicInfo.payChannel] || '手机' }}完成支付</p>

              <!-- payUrl：支付链接（用于跳转或展示二维码） -->
              <div class="pay-link-area" v-if="payResult.payUrl">
                <el-button type="primary" size="large" @click="openPayUrl" style="width: 100%;">
                  前往{{ PayChannelMap[order.basicInfo.payChannel] || '' }}支付
                </el-button>
                <p class="pay-url-hint">
                  或复制链接到浏览器打开：
                  <el-input :model-value="payResult.payUrl" readonly size="small" style="margin-top: 4px;">
                    <template #append>
                      <el-button @click="copyPayUrl">复制</el-button>
                    </template>
                  </el-input>
                </p>
              </div>

              <!-- payParams：渠道专有参数 -->
              <div class="pay-params-area" v-if="payResult.payParams && Object.keys(payResult.payParams).length">
                <el-descriptions :column="1" border size="small" style="margin-top: 12px;" title="支付参数">
                  <el-descriptions-item v-for="(val, key) in payResult.payParams" :key="key" :label="String(key)">
                    <span class="font-mono">{{ val }}</span>
                  </el-descriptions-item>
                </el-descriptions>
              </div>

              <!-- prepayId 展示 -->
              <p class="prepay-id" v-if="payResult.prepayId">
                预支付单号：<span class="font-mono">{{ payResult.prepayId }}</span>
              </p>
            </template>

            <!-- 发起/刷新支付按钮 -->
            <el-button type="primary" size="large" :loading="paying" @click="handlePay" style="width: 100%; margin-top: 16px;">
              {{ payResult ? '已支付？点击刷新状态' : '确认支付' }}
            </el-button>
            <el-button size="large" @click="handleCancel" style="width: 100%; margin-top: 8px;">
              取消订单
            </el-button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tradeApi } from '@/shared/api'
import { PayChannelMap } from '@/shared/constants'
import { formatCurrency, formatTime, generateRequestId } from '@/shared/utils'
import type { OrderDetailVO, PayResultVO } from '@/shared/types'

const props = defineProps<{ orderNo: string }>()
const router = useRouter()
const loading = ref(false)
const paying = ref(false)
const order = ref<OrderDetailVO | null>(null)
const payResult = ref<PayResultVO | null>(null)

async function loadOrder() {
  loading.value = true
  try {
    const res = await tradeApi.getOrderDetail(props.orderNo)
    order.value = res.data
  } finally {
    loading.value = false
  }
}

/** 发起支付 */
async function handlePay() {
  paying.value = true
  try {
    const res = await tradeApi.payOrder(props.orderNo, {
      payChannel: order.value!.basicInfo.payChannel,
      requestId: generateRequestId()
    })
    payResult.value = res.data
    ElMessage.success('支付请求已发起')
    /* 刷新订单状态 */
    loadOrder()
  } finally {
    paying.value = false
  }
}

/** 打开支付链接 */
function openPayUrl() {
  if (payResult.value?.payUrl) {
    window.open(payResult.value.payUrl, '_blank', 'noopener,noreferrer')
  }
}

/** 复制支付链接 */
function copyPayUrl() {
  if (payResult.value?.payUrl) {
    navigator.clipboard.writeText(payResult.value.payUrl).then(() => {
      ElMessage.success('链接已复制')
    })
  }
}

/** 取消订单 */
async function handleCancel() {
  await ElMessageBox.confirm('确定要取消订单吗？', '取消订单', { type: 'warning' })
  await tradeApi.cancelOrder(props.orderNo, generateRequestId())
  ElMessage.success('订单已取消')
  loadOrder()
}

onMounted(loadOrder)
</script>

<style scoped lang="scss">
.pay-card {
  text-align: center;
}
.pay-amount {
  padding: 24px 0;
  &__label {
    display: block;
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }
  &__value {
    font-size: 36px;
    font-weight: 700;
    color: #E6524B;
  }
}
.pay-qr-area {
  padding: 16px 0;
}
.pay-channel-tip {
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
}
.pay-link-area {
  margin-top: 12px;
}
.pay-url-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 12px;
  text-align: left;
}
.pay-params-area {
  text-align: left;
}
.prepay-id {
  font-size: 12px;
  color: #909399;
  margin-top: 12px;
}
</style>
