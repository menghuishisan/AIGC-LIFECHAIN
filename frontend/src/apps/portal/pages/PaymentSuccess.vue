<template>
  <main data-page="PaymentSuccess" class="payment-result-page">
    <section class="lc-card payment-result" aria-live="polite" :aria-busy="checking">
      <el-result :icon="paid ? 'success' : 'info'" :title="title" :sub-title="description">
        <template #extra>
          <div class="result-actions">
            <el-button v-if="canQuery" type="primary" :loading="checking" @click="startQuery"
              >重新查询结果</el-button
            >
            <el-button
              v-if="orderNo && user.isLoggedIn"
              @click="router.push('/orders/' + encodeURIComponent(orderNo))"
              >查看订单</el-button
            >
            <el-button v-if="!user.isLoggedIn" type="primary" @click="router.push('/login')"
              >登录后查看订单</el-button
            >
            <el-button v-else @click="router.push('/buyer/dashboard')">返回我的订单</el-button>
          </div>
        </template>
      </el-result>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/app/store/user'
import { tradeApi } from '@/shared/api'
import { OrderStatusMap } from '@/shared/constants'
import { isPaymentConfirmed, readPendingPayment } from '@/shared/utils/payment'
const router = useRouter()
const user = useUserStore()
// 不使用 return_url 的查询参数判断交易归属或支付结果。
const orderNo = readPendingPayment()
const status = ref('')
const checking = ref(false)
const failed = ref(false)
const paid = computed(() => isPaymentConfirmed(status.value))
const canQuery = computed(() => !!orderNo && user.isLoggedIn && !paid.value)
let timer: ReturnType<typeof setTimeout> | undefined
let disposed = false
let attempts = 0
const title = computed(() => {
  if (paid.value) return '支付已确认'
  if (checking.value) return '正在确认支付结果'
  if (failed.value) return '暂时无法确认支付结果'
  return OrderStatusMap[status.value] || '支付结果待确认'
})
const description = computed(() => {
  if (paid.value) return '支付已由服务端确认，可在订单中查看授权进度。'
  if (!user.isLoggedIn) return '请登录后在我的订单中查询，当前页面不能确认付款是否成功。'
  if (!orderNo) return '未找到本次付款的订单信息，请在我的订单中查询。'
  if (status.value === 'ORDER_EXCEPTION') return '订单需要人工核实，请查看订单详情或联系平台。'
  return '请勿重复付款。付款结果以订单中的确认状态为准。'
})

async function queryResult() {
  checking.value = true
  failed.value = false
  try {
    const result = await tradeApi.refreshPayment(orderNo)
    if (disposed) return
    status.value = result.data.statusInfo.orderStatus
    attempts += 1
    if (status.value === 'PAY_PENDING_CONFIRM' && attempts < 12)
      timer = setTimeout(queryResult, 3000)
  } catch {
    if (!disposed) failed.value = true
  } finally {
    if (!disposed) checking.value = false
  }
}

function startQuery() {
  if (!canQuery.value || checking.value) return
  clearTimeout(timer)
  attempts = 0
  void queryResult()
}
onMounted(startQuery)
onUnmounted(() => {
  disposed = true
  clearTimeout(timer)
})
</script>

<style scoped lang="scss">
.payment-result-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 16px;
  background: var(--el-fill-color-light);
}
.payment-result {
  width: min(100%, 640px);
  box-sizing: border-box;
}
.result-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}
.result-actions .el-button {
  margin-left: 0;
}
</style>
