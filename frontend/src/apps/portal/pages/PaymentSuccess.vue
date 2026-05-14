<template>
  <div class="payment-result-page">
    <el-result
      icon="success"
      title="支付成功"
      sub-title="您的订单已支付成功，授权正在处理中"
    >
      <template #extra>
        <el-button type="primary" @click="goOrder">查看订单</el-button>
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
    router.push(`/buyer/orders/${orderNo}`)
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
