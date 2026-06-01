<template>
  <!-- 购买者工作台 -->
  <div class="buyer-dashboard-page">
    <div class="page-header">
      <h2>购买者工作台</h2>
    </div>

    <!-- 概览指标 -->
    <el-row :gutter="16" class="metrics-row">
      <el-col :span="8">
        <div class="metric-card">
          <div class="metric-card__label">我的订单</div>
          <div class="metric-card__value">{{ stats.orderCount }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="metric-card">
          <div class="metric-card__label">已获授权</div>
          <div class="metric-card__value">{{ stats.licenseCount }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="metric-card">
          <div class="metric-card__label">待支付</div>
          <div class="metric-card__value pending">{{ stats.pendingPayCount }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <div class="lc-card" style="margin-top: 20px;">
      <div class="lc-card__title">快捷操作</div>
      <div class="quick-actions">
        <el-button type="primary" @click="router.push('/market')">浏览市场</el-button>
        <el-button @click="router.push('/buyer/licenses')">我的授权</el-button>
      </div>
    </div>

    <!-- 最近订单 -->
    <div class="lc-card" style="margin-top: 20px;">
      <div class="lc-card__title">最近订单</div>
      <el-table :data="recentOrders" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单编号" min-width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/orders/${row.orderNo}`)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="workTitle" label="作品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="orderStatus" label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.orderStatus" type="order" />
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="金额" width="120">
          <template #default="{ row }">{{ formatCurrency(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.orderStatus === 'ORDER_CREATED'" link type="primary" size="small" @click="router.push(`/orders/${row.orderNo}/pay`)">去支付</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 最新通知 -->
    <div class="lc-card" style="margin-top: 20px;">
      <div class="lc-card__title">
        最新通知
        <el-button link type="primary" style="float: right;" @click="router.push('/notices')">查看全部</el-button>
      </div>
      <el-table :data="notices" stripe size="small">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/notices/${row.noticeNo}`)">
              <el-badge v-if="!row.readFlag" is-dot>{{ row.title }}</el-badge>
              <span v-else>{{ row.title }}</span>
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tradeApi, appApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { formatCurrency, formatTime } from '@/shared/utils'
import type { OrderListVO, MessageNoticeVO } from '@/shared/types'

const router = useRouter()
const loading = ref(false)
const recentOrders = ref<OrderListVO[]>([])
const notices = ref<MessageNoticeVO[]>([])

const stats = reactive({
  orderCount: 0,
  licenseCount: 0,
  pendingPayCount: 0
})

async function loadData() {
  loading.value = true
  try {
    /* 并行加载订单、通知、待支付数 */
    const [orderRes, noticeRes, pendingRes] = await Promise.all([
      tradeApi.getMyOrders({ pageNo: 1, pageSize: 5, role: 'BUYER' }),
      appApi.getNotices({ pageNo: 1, pageSize: 5 }),
      tradeApi.getMyOrders({ pageNo: 1, pageSize: 1, role: 'BUYER', status: 'ORDER_CREATED' })
    ])
    recentOrders.value = orderRes.data.records
    stats.orderCount = orderRes.data.total
    stats.pendingPayCount = pendingRes.data.total
    notices.value = noticeRes.data.records

    /* 加载授权统计 */
    try {
      const licRes = await tradeApi.getMyLicenses({ pageNo: 1, pageSize: 1 })
      stats.licenseCount = licRes.data.total
    } catch { /* 忽略 */ }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 16px;
}
.metrics-row {
  margin-bottom: 8px;
}
.metric-card__value.pending {
  color: #e6a23c;
}
.quick-actions {
  display: flex;
  gap: 12px;
}
</style>
