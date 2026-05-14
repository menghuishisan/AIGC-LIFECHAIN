<template>
  <div class="payment-result-page">
    <el-result
      icon="warning"
      title="支付已取消"
      sub-title="您已取消本次支付，订单仍保留，可重新发起支付"
    >
      <template #extra>
        <el-button type="primary" @click="goOrder">返回订单</el-button>
        <el-button @click="router.push('/market')">返回市场</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

function goOrder() {
  const orderNo = route.query.order_no as string
  if (orderNo) {
    router.push(`/orders/${orderNo}/pay`)
  } else {
    router.push('/buyer/dashboard')
  }
}
</script>

<style scoped lang="scss">
.payment-result-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}
</style>
