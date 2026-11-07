<template>
  <div data-page="PayPage" class="lc-page pay-page" v-loading="loading">
    <div>
      <h1>订单支付</h1>
      <p class="page-header__desc">付款状态以渠道通知或服务端查询结果为准。</p>
    </div>
    <div class="lc-card pay-card">
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <template v-if="order">
        <p class="pay-label">支付金额</p>
        <p class="pay-amount">{{ formatCurrency(order.basicInfo.payAmount) }}</p>
        <el-descriptions :column="1" size="small">
          <el-descriptions-item label="订单编号"
            ><span class="font-mono">{{ order.basicInfo.orderNo }}</span></el-descriptions-item
          >
          <el-descriptions-item label="作品名称">{{
            order.basicInfo.workTitle
          }}</el-descriptions-item>
          <el-descriptions-item label="支付渠道">{{
            PayChannelMap[order.basicInfo.payChannel]
          }}</el-descriptions-item>
          <el-descriptions-item label="过期时间">{{
            formatTime(order.timeInfo.expireTime)
          }}</el-descriptions-item>
        </el-descriptions>
        <div aria-live="polite">
          <el-result v-if="paid" icon="success" title="支付已确认" sub-title="授权进度请查看订单" />
          <el-result
            v-else-if="!canPay"
            icon="info"
            :title="OrderStatusMap[order.statusInfo.orderStatus] || '请查看订单状态'"
          />
          <p v-else class="pay-tip">
            请通过{{ PayChannelMap[order.basicInfo.payChannel] }}完成付款，由系统确认结果。
          </p>
        </div>
        <div class="pay-actions">
          <el-button
            v-if="canPay"
            type="primary"
            size="large"
            :loading="paying"
            :disabled="refreshing"
            @click="handlePay"
          >
            {{ order.statusInfo.orderStatus === 'PAY_PENDING_CONFIRM' ? '继续支付' : '确认支付' }}
          </el-button>
          <template v-if="wechatUrl && order.statusInfo.orderStatus === 'PAY_PENDING_CONFIRM'">
            <el-button type="primary" @click="openWechat">打开微信支付</el-button>
            <el-button @click="copyWechatLink">复制微信付款链接</el-button>
          </template>
          <el-button
            v-if="order.statusInfo.orderStatus === 'PAY_PENDING_CONFIRM'"
            :loading="refreshing"
            :disabled="paying"
            @click="refreshStatus"
            >我已付款，查询结果</el-button
          >
          <el-button
            v-if="canPay"
            :loading="cancelling"
            :disabled="paying || refreshing"
            @click="handleCancel"
            >取消订单</el-button
          >
          <el-button @click="router.push('/orders/' + encodeURIComponent(orderNo))"
            >查看订单</el-button
          >
        </div>
      </template>
      <el-empty v-else-if="!loading" description="订单暂时无法加载">
        <el-button @click="loadOrder">重新加载</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tradeApi } from '@/shared/api'
import { OrderStatusMap, PayChannelMap } from '@/shared/constants'
import { formatCurrency, formatTime, generateRequestId } from '@/shared/utils'
import { isPaymentConfirmed, submitAlipayForm } from '@/shared/utils/payment'
import type { OrderDetailVO } from '@/shared/types'
const props = defineProps<{ orderNo: string }>()
const router = useRouter()
const loading = ref(false)
const paying = ref(false)
const refreshing = ref(false)
const cancelling = ref(false)
const error = ref('')
const wechatUrl = ref('')
const order = ref<OrderDetailVO | null>(null)
const paid = computed(() => isPaymentConfirmed(order.value?.statusInfo.orderStatus || ''))
const canPay = computed(
  () =>
    order.value?.statusInfo.orderStatus === 'ORDER_CREATED' ||
    (order.value?.statusInfo.orderStatus === 'PAY_PENDING_CONFIRM' &&
      order.value.basicInfo.payChannel === 'ALIPAY')
)

async function loadOrder() {
  loading.value = true
  error.value = ''
  try {
    order.value = (await tradeApi.getOrderDetail(props.orderNo)).data
  } catch {
    error.value = '订单加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

async function handlePay() {
  if (paying.value) return
  if (!order.value) return
  paying.value = true
  error.value = ''
  try {
    const channel = order.value.basicInfo.payChannel
    const result = (
      await tradeApi.payOrder(props.orderNo, {
        payChannel: channel,
        requestId: generateRequestId()
      })
    ).data
    if (channel === 'ALIPAY') {
      if (!result.formHtml) throw new Error('未获取到支付表单，请稍后重试')
      submitAlipayForm(result.formHtml, props.orderNo)
    } else if (channel === 'WECHAT_PAY') {
      const url = new URL(result.payUrl || '')
      if (
        url.protocol !== 'weixin:' ||
        url.hostname !== 'wxpay' ||
        url.pathname !== '/bizpayurl' ||
        url.username ||
        url.password ||
        url.port ||
        url.hash
      )
        throw new Error('微信付款链接无效')
      wechatUrl.value = url.href
      await loadOrder()
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '支付发起失败，请稍后重试。'
  } finally {
    paying.value = false
  }
}

function openWechat() {
  window.location.assign(wechatUrl.value)
}
async function copyWechatLink() {
  try {
    await navigator.clipboard.writeText(wechatUrl.value)
    ElMessage.success('付款链接已复制')
  } catch {
    error.value = '复制失败，请打开微信支付。'
  }
}

async function refreshStatus() {
  refreshing.value = true
  error.value = ''
  try {
    order.value = (await tradeApi.refreshPayment(props.orderNo)).data
  } catch {
    error.value = '暂时无法确认支付结果，请稍后查询。'
  } finally {
    refreshing.value = false
  }
}

async function handleCancel() {
  try {
    if (
      !(await ElMessageBox.confirm('确定取消该订单？系统会先核实支付状态。', '取消订单', {
        type: 'warning'
      })
        .then(() => true)
        .catch(() => false))
    )
      return
  } catch {
    return
  }
  cancelling.value = true
  try {
    await tradeApi.cancelOrder(props.orderNo, generateRequestId())
    ElMessage.success('订单已取消')
    await loadOrder()
  } catch {
    error.value = '取消未完成，请先查询支付结果。'
  } finally {
    cancelling.value = false
  }
}

onMounted(loadOrder)
</script>

<style scoped lang="scss">
.pay-page h2 {
  margin-bottom: 20px;
}
.pay-card {
  max-width: 560px;
  margin: 0 auto;
  overflow-wrap: anywhere;
}
.pay-label {
  margin-top: 24px;
  text-align: center;
  color: var(--el-text-color-secondary);
}
.pay-amount {
  margin: 12px 0 24px;
  font-size: 36px;
  font-weight: 700;
  text-align: center;
}
.pay-tip {
  margin: 24px 0;
  color: var(--el-text-color-regular);
}
.pay-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.pay-actions .el-button {
  margin-left: 0;
}
</style>
